package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record LoanEstimateRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        int tenureMonths,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monthlyIncome,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal existingMonthlyEmi
) {
}
