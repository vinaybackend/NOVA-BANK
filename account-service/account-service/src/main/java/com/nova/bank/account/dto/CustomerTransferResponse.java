package com.nova.bank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerTransferResponse {
    private String customerId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String currency;
}
