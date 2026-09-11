package com.nova.bank.account.dto;

import com.nova.bank.account.entities.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 30, message = "Customer ID must not exceed 30 characters")
    private String customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;
}
