package com.nova.bank.transaction.producer;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class TransactionEvent {
    String eventId;
    String transactionId;
    String fromAccountNumber;
    String toAccountNumber;
    String customerId;
    String customerName;
    String email;
    String  transactionType;
    BigDecimal amount;
    String currency;
    String transactionStatus;
    String reference;
    String description;
    String performedBy;


}
