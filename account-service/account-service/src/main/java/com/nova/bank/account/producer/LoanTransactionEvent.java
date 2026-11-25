package com.nova.bank.account.producer;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanTransactionEvent {
    private String eventId;
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String customerId;
    private String customerName;
    private String email;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String transactionStatus;
    private String reference;
    private String description;
    private String performedBy;
    private String branchCode;
}

