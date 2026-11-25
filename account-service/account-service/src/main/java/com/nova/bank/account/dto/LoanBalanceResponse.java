package com.nova.bank.account.dto;

import java.math.BigDecimal;

public record LoanBalanceResponse(
        String accountNumber,
        String customerId,
        String currency,
        BigDecimal balance
) {
}
