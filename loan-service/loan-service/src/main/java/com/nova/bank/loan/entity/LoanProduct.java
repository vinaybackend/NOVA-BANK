package com.nova.bank.loan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_products")
@Getter
@Setter
@NoArgsConstructor
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private int minimumTenureMonths;

    @Column(nullable = false)
    private int maximumTenureMonths;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maximumFoirPercent;

    @Column(nullable = false)
    private int minimumCreditScore;

    @Column(nullable = false)
    private boolean active = true;
}
