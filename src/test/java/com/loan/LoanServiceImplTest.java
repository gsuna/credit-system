package com.loan;

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
import com.loan.service.SecurityAccessService;
import com.loan.service.cache.RedissonCacheService;
import com.loan.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RedissonCacheService redissonCacheService;

    @Mock
    private LoanMapper loanMapper;

    @Mock
    private LoanInstallmentMapper loanInstallmentMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SecurityAccessService securityAccessService;

    @Mock
    private RLock lock;

    @InjectMocks
    private LoanServiceImpl loanService;

    private final Long CUSTOMER_ID = 1L;
    private final Long LOAN_ID = 1L;
    private final BigDecimal AMOUNT = new BigDecimal("10000");
    private final BigDecimal INTEREST_RATE = new BigDecimal("0.2");
    private final int INSTALLMENT_COUNT = 12;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loanService, "maxInstallmentPayment", 3);
    }

    @Test
    void createLoan_Success() throws InterruptedException {
        CreateLoanRequest request = new CreateLoanRequest(CUSTOMER_ID, AMOUNT, INTEREST_RATE, INSTALLMENT_COUNT);
        CustomerDto customerDto = CustomerDto.builder()
                .id(CUSTOMER_ID)
                .creditLimit(new BigDecimal("20000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .build();

        Customer customerEntity = new Customer();
        customerEntity.setId(CUSTOMER_ID);

        Loan loan = new Loan();
        loan.setId(LOAN_ID);
        loan.setCustomer(customerEntity);

        LoanDto loanDto = LoanDto.builder()
                .id(LOAN_ID)
                .customer(customerDto)
                .build();

        when(redissonCacheService.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(customerService.findById(CUSTOMER_ID)).thenReturn(customerDto);
        when(customerMapper.toEntity(customerDto)).thenReturn(customerEntity);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toDto(loan)).thenReturn(loanDto);

        when(customerRepository.save(any(Customer.class))).thenReturn(customerEntity);

        LoanDto result = loanService.createLoan(request);

        assertNotNull(result);
        verify(loanRepository).save(any(Loan.class));
        verify(loanInstallmentRepository).saveAll(anyList());
        verify(redissonCacheService).put(anyString(), any(LoanDto.class), anyLong());
        verify(lock).unlock();
    }

    @Test
    void createLoan_InsufficientCredit_ThrowsException() throws InterruptedException {
        CreateLoanRequest request = new CreateLoanRequest(CUSTOMER_ID, AMOUNT, INTEREST_RATE, INSTALLMENT_COUNT);
        CustomerDto customerDto = CustomerDto.builder()
                .creditLimit(new BigDecimal("5000"))
                .usedCreditLimit(BigDecimal.ZERO)
                .build();

        when(redissonCacheService.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(customerService.findById(CUSTOMER_ID)).thenReturn(customerDto);

        assertThrows(BusinessException.class, () -> loanService.createLoan(request));
        verify(lock).unlock();
    }

    @Test
    void getCustomerLoans_ReturnsCachedData() {
        String cacheKey = RedisConstants.CUSTOMER_LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + CUSTOMER_ID;
        List<LoanDto> cachedLoans = Collections.singletonList(LoanDto.builder().build());

        when(redissonCacheService.get(cacheKey)).thenReturn(cachedLoans);

        List<LoanDto> result = loanService.getCustomerLoans(CUSTOMER_ID);

        assertEquals(1, result.size());
        verify(redissonCacheService).get(cacheKey);
        verify(loanRepository, never()).findByCustomerId(anyLong());
    }

    @Test
    void getCustomerLoans_FetchesFromDbWhenCacheEmpty() {
        String cacheKey = RedisConstants.CUSTOMER_LOAN_PREFIX + RedisConstants.SPLIT_PREFIX + CUSTOMER_ID;
        List<Loan> loans = Collections.singletonList(new Loan());

        when(redissonCacheService.get(cacheKey)).thenReturn(null);
        when(loanRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(loans);
        when(loanMapper.toDtoList(loans)).thenReturn(Collections.singletonList(LoanDto.builder().build()));

        List<LoanDto> result = loanService.getCustomerLoans(CUSTOMER_ID);

        assertEquals(1, result.size());
        verify(redissonCacheService).put(eq(cacheKey), anyList(), anyLong());
    }

    @Test
    void doPayment_PaysSingleInstallment() throws InterruptedException {
        PaymentRequest request = new PaymentRequest(LOAN_ID, new BigDecimal("1000"));
        CustomerDto customerDto = CustomerDto.builder()
                .id(CUSTOMER_ID)
                .build();

        LoanDto loanDto = LoanDto.builder()
                .id(LOAN_ID)
                .customer(customerDto)
                .build();


        Long installmentId = 1L;
        LoanInstallment installment = new LoanInstallment();
        installment.setId(installmentId);
        installment.setAmount(new BigDecimal("1000"));
        installment.setDueDate(new Date());

        LoanInstallmentDto installmentDto = LoanInstallmentDto.builder()
                .id(installmentId)
                .amount(new BigDecimal("1000"))
                .dueDate(new Date()).build();

        when(redissonCacheService.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(new Loan()));
        when(loanMapper.toDto(any(Loan.class))).thenReturn(loanDto);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateIsBeforeOrderByInstallmentNumberAsc(
                anyLong(), any(Date.class))).thenReturn(Collections.singletonList(installment));
        when(loanInstallmentMapper.toDto(installment)).thenReturn(installmentDto);
        when(loanInstallmentRepository.save(any(LoanInstallment.class))).thenReturn(installment);

        PaymentResponse response = loanService.doPayment(request);

        assertEquals(1, response.getNumberOfPaidInstallment());
        assertEquals(0, response.getRemainingAmount().compareTo(BigDecimal.ZERO));
        verify(loanInstallmentRepository).save(any(LoanInstallment.class));
        verify(lock).unlock();
    }

    @Test
    void doPayment_ThrowsWhenNoUnpaidInstallments() throws InterruptedException {
        PaymentRequest request = new PaymentRequest(LOAN_ID, new BigDecimal("1000"));
        LoanDto loanDto = LoanDto.builder()
                .id(LOAN_ID)
                .customer(CustomerDto.builder().build())
                .build();

        when(redissonCacheService.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(10, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(new Loan()));
        when(loanMapper.toDto(any(Loan.class))).thenReturn(loanDto);
        when(loanInstallmentRepository.findByLoanIdAndIsPaidFalseAndDueDateIsBeforeOrderByInstallmentNumberAsc(
                anyLong(), any(Date.class))).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> loanService.doPayment(request));
        verify(lock).unlock();
    }


    @Test
    void findById_LoanExists_ReturnsLoanDto() {
        Loan loan = new Loan();
        loan.setId(LOAN_ID);

        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        when(loanMapper.toDto(loan)).thenReturn(LoanDto.builder().build());

        LoanDto result = loanService.findById(LOAN_ID);

        assertNotNull(result);
    }

    @Test
    void findById_LoanNotExists_ThrowsException() {
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> loanService.findById(LOAN_ID));
    }
}