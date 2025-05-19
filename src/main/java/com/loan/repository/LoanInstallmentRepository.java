package com.loan.repository;

import com.loan.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment,Long> {

    List<LoanInstallment> findByLoanIdAndIsPaidFalseAndDueDateIsBeforeOrderByInstallmentNumberAsc(Long loanId, Date maxDueDate);

    Integer countByLoanIdAndIsPaidFalse(Long id);
}
