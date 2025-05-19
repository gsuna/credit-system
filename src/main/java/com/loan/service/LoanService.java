package com.loan.service;

import com.loan.dto.LoanDto;
import com.loan.dto.request.CreateLoanRequest;
import com.loan.dto.request.PaymentRequest;
import com.loan.dto.response.PaymentResponse;

import java.util.List;

public interface LoanService {

    LoanDto findById(Long id);
    LoanDto createLoan(CreateLoanRequest request);
    List<LoanDto> getCustomerLoans(Long customerId);

    PaymentResponse doPayment(PaymentRequest request);
}
