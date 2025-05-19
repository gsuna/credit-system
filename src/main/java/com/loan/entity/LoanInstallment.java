package com.loan.entity;

import com.loan.converter.DateConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_installments")
public class LoanInstallment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal paidAmount;

    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    @Convert(converter = DateConverter.class)
    private Date dueDate;

    @Column(columnDefinition = "VARCHAR(10)")
    @Convert(converter = DateConverter.class)
    private Date paymentDate;

    @Column(nullable = false)
    private Boolean isPaid;

}
