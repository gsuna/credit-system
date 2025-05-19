package com.loan.service.impl;

import com.loan.constants.RedisConstants;
import com.loan.dto.CustomerDto;
import com.loan.dto.LoanDto;
import com.loan.dto.LoanInstallmentDto;
import com.loan.dto.request.CreateLoanRequest;
import com.loan.dto.request.PaymentRequest;
import com.loan.dto.response.PaymentResponse;
import com.loan.entity.Customer;
import com.loan.entity.Loan;
import com.loan.entity.LoanInstallment;
import com.loan.exception.BusinessException;
import com.loan.mappers.CustomerMapper;
import com.loan.mappers.LoanInstallmentMapper;
import com.loan.mappers.LoanMapper;
import com.loan.repository.CustomerRepository;
import com.loan.repository.LoanInstallmentRepository;
import com.loan.repository.LoanRepository;
import com.loan.service.CustomerService;
import com.loan.service.LoanService;
import com.loan.service.SecurityAccessService;
import com.loan.service.cache.RedissonCacheService;
import com.loan.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final RedissonCacheService redissonCacheService;
    private final LoanMapper loanMapper;
    private final LoanInstallmentMapper loanInstallmentMapper;
    private final CustomerMapper customerMapper;
    private final SecurityAccessService securityAccessService;

    @Value("${loan.max-installment.payment}")
    private Integer maxInstallmentPayment;

    public LoanDto createLoan(CreateLoanRequest request) {
        String lockKey = RedisConstants.LOAN_LOCK_PREFIX + RedisConstants.SPLIT_PREFIX + request.getCustomerId();
        RLock lock = redissonCacheService.getLock(lockKey);

        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("Creating loan failed. Please try again.");
            }

            CustomerDto customer = customerService.findById(request.getCustomerId());
            // loan validations
            validateCreditRules(request, customer);

            BigDecimal totalAmount = request.getAmount().multiply(
                    BigDecimal.ONE.add(request.getInterestRate()));

            Customer customerEntity = customerMapper.toEntity(customer);

            Loan loan = Loan.builder()
                    .customer(customerEntity)
                    .loanAmount(request.getAmount())
                    .interestRate(request.getInterestRate())
                    .totalAmount(totalAmount)
                    .numberOfInstallment(request.getNumberOfInstallments())
                    .isPaid(false)
                    .build();

            // save loan installments
            BigDecimal installmentAmount = totalAmount.divide(
                    BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);

            List<LoanInstallment> installments = IntStream.range(0, request.getNumberOfInstallments())
                    .mapToObj(i -> {
                        Date dueDate = DateUtils.addMonthsToDate(
                                DateUtils.getFirstDayOfNextMonth(), i);

                        return LoanInstallment.builder()
                                .loan(loan)
                                .installmentNumber(i+1)
                                .amount(installmentAmount)
                                .paidAmount(null)
                                .dueDate(dueDate)
                                .paymentDate(null)
                                .isPaid(false)
                                .build();
                    })
                    .collect(Collectors.toList());

            // update loan installments
            customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(request.getAmount()));
            customerRepository.save(customerEntity);

            //save loan to db
            Loan savedLoan = loanRepository.save(loan);
            loanInstallmentRepository.saveAll(installments);

            // save cache
            cacheLoanDetails(loanMapper.toDto(savedLoan));

            //convert to dto
            return loanMapper.toDto(savedLoan);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("While creating process loan has an error");
        } finally {
            lock.unlock();
        }
    }

    private static void validateCreditRules(CreateLoanRequest request, CustomerDto customer) {
        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());
        if (availableCredit.compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient credit limit");
        }

        if (request.getInterestRate().compareTo(BigDecimal.valueOf(0.1)) < 0 ||
                request.getInterestRate().compareTo(BigDecimal.valueOf(0.5)) > 0) {
            throw new BusinessException("Invalid interest rate (it should be between 0.1 and 0.5)");
        }

        if (!List.of(6, 9, 12, 24).contains(request.getNumberOfInstallments())) {
            throw new BusinessException("Invalid installment count (6,9,12,24)");
        }
    }

    public List<LoanDto> getCustomerLoans(Long customerId) {
        securityAccessService.isAdminOrOwner(customerId);
        String customerCreditPrefix = RedisConstants.CUSTOMER_LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + customerId;
        long customerCreditTtl = 60 * 30 * 24 ;

        // fetch from cache
        List<LoanDto> cachedLoans = redissonCacheService.get(customerCreditPrefix);
        if (cachedLoans!=null) {
            return cachedLoans;
        }

        // if not present in cache fetch from db
        List<Loan> loans = loanRepository.findByCustomerId(customerId);
        List<LoanDto> loanDtos = loanMapper.toDtoList(loans);

        // saving cache
        redissonCacheService.put(customerCreditPrefix, loanDtos, customerCreditTtl);

        return loanDtos;
    }

    @Override
    public LoanDto findById(Long id) {
        return loanMapper.toDto(loanRepository.findById(id).orElseThrow(()-> new BusinessException("Loan not found")));
    }

    @Transactional
    public PaymentResponse doPayment(PaymentRequest request) {
        String lockKey = RedisConstants.LOAN_LOCK_PREFIX + RedisConstants.SPLIT_PREFIX + request.getLoanId();
        RLock lock = redissonCacheService.getLock(lockKey);

        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BusinessException("Payment transaction timed out");
            }

            LoanDto loan = findById(request.getLoanId());
            securityAccessService.isAdminOrOwner(loan.getCustomer().getId());

            String formattedDate = DateUtils.formatDate(DateUtils.addMonthsToDate(new Date(), maxInstallmentPayment));
            Date maxPaymentDate = DateUtils.convertDate(formattedDate);
            List<LoanInstallment> unpaidInstallments = loanInstallmentRepository
                    .findByLoanIdAndIsPaidFalseAndDueDateIsBeforeOrderByInstallmentNumberAsc(request.getLoanId(), maxPaymentDate);


            if (unpaidInstallments.isEmpty()) {
                throw new BusinessException("No unpaid installments");
            }

            BigDecimal remainingAmount = request.getAmount();
            int paidInstallments = 0;

            List<LoanInstallmentDto> loanInstallmentDtoList = new ArrayList<>();
            for (LoanInstallment installment : unpaidInstallments) {
                BigDecimal calculatedPaymentAmount = calculateFinalAmount(installment);
                if (remainingAmount.compareTo(calculatedPaymentAmount) >= 0 && maxInstallmentPayment>paidInstallments) {
                    // installment payment
                    installment.setPaidAmount(calculatedPaymentAmount);
                    installment.setPaymentDate(new Date());
                    installment.setIsPaid(true);
                    installment = loanInstallmentRepository.save(installment);

                    remainingAmount = remainingAmount.subtract(calculatedPaymentAmount);
                    paidInstallments++;
                    loanInstallmentDtoList.add(loanInstallmentMapper.toDto(installment));
                } else {
                    break;
                }
            }

            // if all installments paid update loan isPaid to true
            boolean allPaid = loanInstallmentRepository.countByLoanIdAndIsPaidFalse(loan.getId()) == 0;
            if (allPaid) {
                loan.setIsPaid(true);
                loanRepository.save(loanMapper.toEntity(loan));
            }

            // update cache
            evictLoanCache(loan.getId(), loan.getCustomer().getId());

            String.format("%d installments paid. Remaining amount: %.2f",
                    paidInstallments, remainingAmount);
            return PaymentResponse.builder().totalAmount(request.getAmount())
                    .remainingAmount(remainingAmount)
                    .paidLoanInstallmentList(loanInstallmentDtoList)
                    .numberOfPaidInstallment(paidInstallments)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Paid transaction interrupted");
        } finally {
            lock.unlock();
        }
    }

    private BigDecimal calculateFinalAmount(LoanInstallment installment) {
        Date now = new Date();
        String formattedDate = DateUtils.formatDate(now);
        Date maxPaymentDate = DateUtils.convertDate(formattedDate);
        long daysDiff = DateUtils.daysBetween(maxPaymentDate, installment.getDueDate());
        BigDecimal adjustment = installment.getAmount()
                .multiply(BigDecimal.valueOf(0.001))
                .multiply(BigDecimal.valueOf(Math.abs(daysDiff)));

        if (daysDiff > 0) {
            // discount
            adjustment = adjustment.negate();
        }

        return installment.getAmount().add(adjustment);
    }

    private void cacheLoanDetails(LoanDto loan) {
        String loanCacheKey = RedisConstants.LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + loan.getId();
        String customerLoanCacheKey = RedisConstants.CUSTOMER_LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + loan.getCustomer().getId();
        long loanCacheTtl = 30 * 60;

        // cache loan
        redissonCacheService.put(loanCacheKey, loan, loanCacheTtl);
        redissonCacheService.delete(customerLoanCacheKey);
    }

    private void evictLoanCache(Long loanId, Long customerId) {
        String loanCacheKey = RedisConstants.LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + loanId;
        String customerLoanCacheKey = RedisConstants.CUSTOMER_LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + customerId;

        // delete from cache
        redissonCacheService.delete(loanCacheKey);
        redissonCacheService.delete(customerLoanCacheKey);
    }

}
