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
public class PaymentRequest {

    @NotNull(message = "Kredi ID boş olamaz")
    private Long loanId;

    @NotNull(message = "Ödeme tutarı boş olamaz")
    @DecimalMin(value = "10.00", message = "Minimum ödeme tutarı 10 TL olmalıdır")
    @DecimalMax(value = "100000.00", message = "Maksimum ödeme tutarı 100.000 TL olmalıdır")
    private BigDecimal amount;
}
