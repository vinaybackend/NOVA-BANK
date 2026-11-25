package com.nova.bank.loan.dto;

import java.math.BigDecimal;

public record LoanBalanceRequest(
        String operationId,
        String customerId,
        String accountNumber,
        BigDecimal amount
) {
}
