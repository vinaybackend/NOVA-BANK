package com.nova.bank.account.dto;

import com.nova.bank.account.entities.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}