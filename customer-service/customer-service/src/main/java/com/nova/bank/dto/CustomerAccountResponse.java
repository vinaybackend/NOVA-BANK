package com.nova.bank.dto;

import com.nova.bank.client.AccountResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerAccountResponse {
    private AccountResponse account;
}