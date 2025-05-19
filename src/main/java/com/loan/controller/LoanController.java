package com.loan.controller;

import com.loan.annotation.IsAdminOrOwner;
import com.loan.dto.LoanDto;
import com.loan.dto.request.CreateLoanRequest;
import com.loan.dto.request.PaymentRequest;
import com.loan.dto.response.GenericResponse;
import com.loan.dto.response.PaymentResponse;
import com.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {


    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Yeni kredi oluştur", description = "Sadece ADMIN yetkisi gerektirir")
    public ResponseEntity<LoanDto> createLoan(@RequestBody @Valid CreateLoanRequest request) {
        return ResponseEntity.ok(loanService.createLoan(request));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Müşteri kredilerini listele",
            description = "ADMIN tüm müşterileri, CUSTOMER sadece kendini görebilir")
    public ResponseEntity<GenericResponse<List<LoanDto>>> getCustomerLoans(@PathVariable Long customerId) {

        return ResponseEntity.ok(GenericResponse.<List<LoanDto>>builder()
                .data(loanService.getCustomerLoans(customerId))
                .build());
    }

    @PostMapping("/pay")
    @Operation(summary = "Kredi taksiti öde",
            description = "ADMIN tüm krediler için, CUSTOMER sadece kendi kredileri için ödeme yapabilir")
    public ResponseEntity<GenericResponse<PaymentResponse>> doPayment(
            @RequestBody @Valid PaymentRequest request) {

        return ResponseEntity.ok(GenericResponse.<PaymentResponse>builder()
                .data(loanService.doPayment(request))
                .build());
    }
}
