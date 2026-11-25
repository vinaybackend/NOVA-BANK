package com.nova.bank.loan.dto;

import java.math.BigDecimal;

public record LoanBalanceResponse(
        String accountNumber,
        String customerId,
        String currency,
        BigDecimal balance
) {
}
