package com.nova.bank.loan.dto;

public record CustomerBankAccountResponse(
        String accountNumber,
        String customerId,
        String accountType,
        String accountStatus,
        String currency
) {
}
