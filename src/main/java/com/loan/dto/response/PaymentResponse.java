package com.loan.dto.response;


import com.loan.dto.LoanInstallmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private int numberOfPaidInstallment;
    private List<LoanInstallmentDto> paidLoanInstallmentList;


}
