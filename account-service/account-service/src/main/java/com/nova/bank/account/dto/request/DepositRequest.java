package com.nova.bank.account.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Deposit amount must be greater than zero")
    private BigDecimal amount;
}