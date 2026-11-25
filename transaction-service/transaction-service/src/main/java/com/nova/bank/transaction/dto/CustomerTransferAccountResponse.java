package com.nova.bank.transaction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomerTransferAccountResponse {
    private String customerId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String currency;
}
