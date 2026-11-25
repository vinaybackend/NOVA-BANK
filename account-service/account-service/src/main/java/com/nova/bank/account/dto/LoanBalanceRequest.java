package com.nova.bank.account.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LoanBalanceRequest(
        @NotBlank @Size(max = 80) String operationId,
        @NotBlank @Size(max = 30) String customerId,
        @Size(max = 20) String accountNumber,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount
) {
}
