package com.nova.bank.transaction.dto;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransactionResponse {

    private String transactionId;

    private TransactionType transactionType;

    private String fromAccountNumber;

    private String toAccountNumber;

    private String customerId;

    private BigDecimal amount;

    private String currency;

    private TransactionStatus transactionStatus;

    private String reference;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}