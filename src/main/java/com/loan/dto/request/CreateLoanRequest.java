package com.loan.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRequest {

    @NotNull(message = "Customer ID cannot be empty")
    private Long customerId;

    @NotNull(message = "Loan amount cannot be empty")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount must be 1000 TL")
    @DecimalMax(value = "1000000.00", message = "Maximum loan amount must be 1,000,000 TL")
    private BigDecimal amount;

    @NotNull(message = "Interest rate cannot be empty")
    @DecimalMin(value = "0.10", message = "Minimum interest rate must be 10%")
    @DecimalMax(value = "0.50", message = "Maximum interest rate must be 50%")
    private BigDecimal interestRate;

    @NotNull(message = "Number of installments cannot be empty")
    @Min(value = 6, message = "Minimum number of installments must be 6")
    @Max(value = 24, message = "Maximum number of installments must be 24")
    private Integer numberOfInstallments;
}
