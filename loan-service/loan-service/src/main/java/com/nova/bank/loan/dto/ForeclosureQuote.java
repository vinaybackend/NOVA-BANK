package com.nova.bank.loan.dto;

import java.math.BigDecimal;

public record ForeclosureQuote(String loanAccountId, BigDecimal outstandingPrincipal) {
}
