package com.nova.bank.loan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LoanAccount {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, unique = true, length = 36)
    private String applicationId;

    @Column(nullable = false, length = 40)
    private String customerId;

    @Column(nullable = false, length = 20)
    private String bankAccountNumber;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyEmi;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = true)
    private LocalDate disbursedOn;

    @Column(nullable = false)
    private boolean closed;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";
}
