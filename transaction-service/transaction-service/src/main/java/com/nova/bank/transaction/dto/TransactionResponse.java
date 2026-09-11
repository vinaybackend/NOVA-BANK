package com.nova.bank.transaction.dto;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private String transactionId;

    private String accountNumber;

    private String customerId;

    private TransactionType transactionType;

    private BigDecimal amount;

    private String currency;

    private TransactionStatus transactionStatus;

    private String reference;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
