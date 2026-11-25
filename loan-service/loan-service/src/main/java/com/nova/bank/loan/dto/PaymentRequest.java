package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank @Size(max = 36) String paymentId,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount
) {
}
