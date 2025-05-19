package com.loan.config;

import com.loan.CreditSystemApplication;
import com.loan.entity.Customer;
import com.loan.entity.Loan;
import com.loan.entity.LoanInstallment;
import com.loan.entity.User;
import com.loan.enums.RoleType;
import com.loan.repository.CustomerRepository;
import com.loan.repository.LoanInstallmentRepository;
import com.loan.repository.LoanRepository;
import com.loan.repository.UserRepository;
import com.loan.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@RequiredArgsConstructor
public class CommandLineConfig {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner demoData() {
        return args -> {
            System.out.println("Uygulama başladı! CommandLineRunner çalışıyor...");
            System.out.println("Gelen argümanlar: " + String.join(", ", args));

            Customer customer1 = Customer.builder()
                    .name("Ahmet")
                    .surname("Can")
                    .creditLimit(new BigDecimal("50000"))
                    .usedCreditLimit(BigDecimal.ZERO)
                    .build();

            Customer customer2 = Customer.builder()
                    .name("Mehmet")
                    .surname("Ay")
                    .creditLimit(new BigDecimal("75000"))
                    .usedCreditLimit(BigDecimal.ZERO)
                    .build();

            List<Customer> savedCustomers = customerRepository.saveAll(Arrays.asList(customer1, customer2));

            // create user
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(RoleType.ADMIN)
                    .build();

            User user1 = User.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer"))
                    .role(RoleType.CUSTOMER)
                    .customer(savedCustomers.get(0))
                    .build();

            userRepository.saveAll(Arrays.asList(admin, user1));

            // sample loan installments
            createSampleLoan(savedCustomers.get(0), new BigDecimal("10000"), new BigDecimal("0.15"), 12);
            createSampleLoan(savedCustomers.get(0), new BigDecimal("20000"), new BigDecimal("0.20"), 6);
            createSampleLoan(savedCustomers.get(1), new BigDecimal("15000"), new BigDecimal("0.10"), 24);


        };
    }


    private void createSampleLoan(Customer customer, BigDecimal amount, BigDecimal interestRate, int numberOfInstallments) {
        BigDecimal totalAmount = amount.multiply(BigDecimal.ONE.add(interestRate));
        BigDecimal installmentAmount = totalAmount.divide(new BigDecimal(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);

        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(amount)
                .interestRate(interestRate)
                .totalAmount(totalAmount)
                .numberOfInstallment(numberOfInstallments)
                .isPaid(false)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        // create and save installments
        List<LoanInstallment> installments = IntStream.range(0, numberOfInstallments)
                .mapToObj(i -> {
                    Date dueDate = DateUtils.addMonthsToDate(DateUtils.getFirstDayOfNextMonth(), i);

                    return LoanInstallment.builder()
                            .loan(savedLoan)
                            .installmentNumber(i+1)
                            .amount(installmentAmount)
                            .paidAmount(null)
                            .dueDate(dueDate)
                            .paymentDate(null)
                            .isPaid(false)
                            .build();
                })
                .collect(Collectors.toList());

        installmentRepository.saveAll(installments);

        // Update used limit
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(amount));
        customerRepository.save(customer);
    }
}
