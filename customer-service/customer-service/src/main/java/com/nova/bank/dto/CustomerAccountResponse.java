package com.nova.bank.dto;

import com.nova.bank.client.AccountResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerAccountResponse {
    private AccountResponse account;
    private List<AccountResponse> accounts;
}