package com.nova.bank.loan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateLoanProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal minimumAmount,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal maximumAmount,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal annualInterestRate,
        @Min(1) @Max(480) int minimumTenureMonths,
        @Min(1) @Max(480) int maximumTenureMonths,
        @NotNull @DecimalMin("1.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal maximumFoirPercent,
        @Min(300) @Max(900) int minimumCreditScore
) {
}
