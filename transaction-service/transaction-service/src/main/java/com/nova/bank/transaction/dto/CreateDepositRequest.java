package com.nova.bank.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CreateDepositRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @DecimalMax(value = "9999999999999.99", message = "Amount is too large")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;
}