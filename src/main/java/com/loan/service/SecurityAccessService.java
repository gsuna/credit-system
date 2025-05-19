package com.loan.service;

import com.loan.dto.LoanDto;
import com.loan.dto.request.CreateLoanRequest;
import com.loan.dto.request.PaymentRequest;
import com.loan.dto.response.PaymentResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface SecurityAccessService {

    boolean isAdminOrOwner(Long customerId);
}
