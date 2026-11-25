package com.nova.bank.loan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_payments")
@Getter
@Setter
@NoArgsConstructor
public class LoanPayment {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 50)
    private String loanAccountId;

    @Column(nullable = false, length = 20)
    private String paymentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingAfterPayment;

    @Column(nullable = false)
    private LocalDateTime paidAt;
}