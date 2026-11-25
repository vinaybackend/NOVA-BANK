package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ApplyForLoanRequest(
        @NotNull Long productId,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        int tenureMonths,
        @NotBlank String purpose
) {
}
