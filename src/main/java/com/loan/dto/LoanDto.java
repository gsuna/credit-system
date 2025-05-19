package com.loan.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class LoanDto extends BaseDto{
    private CustomerDto customer;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private BigDecimal totalAmount;
    private Integer numberOfInstallment;
    private Date createDate;
    private Boolean isPaid;
}
