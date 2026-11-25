package com.nova.bank.loan.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LoanRecommendationRequest(
        @NotBlank String recommendation,
        @NotBlank String remarks,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Positive @Digits(integer = 3, fraction = 2) BigDecimal annualInterestRate,
        @Positive Integer tenureMonths
) {
}
