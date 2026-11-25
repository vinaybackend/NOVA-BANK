package com.nova.bank.loan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanPortfolioResponse(
        String accountId,
        String applicationId,
        String customerId,
        String customerName,
        String loanStatus,
        BigDecimal principal,
        BigDecimal outstandingPrincipal,
        BigDecimal monthlyEmi,
        LocalDate nextDueDate,
        BigDecimal nextEmiAmount,
        long pendingEmis,
        long overdueEmis,
        long paidEmis,
        BigDecimal totalPaid,
        LocalDateTime lastPaymentAt
) {}

