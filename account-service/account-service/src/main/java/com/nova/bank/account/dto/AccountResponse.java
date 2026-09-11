package com.nova.bank.account.dto;

import com.nova.bank.account.entities.AccountStatus;
import com.nova.bank.account.entities.AccountType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountResponse {

    private String accountId;
    private String accountNumber;
    private String customerId;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}