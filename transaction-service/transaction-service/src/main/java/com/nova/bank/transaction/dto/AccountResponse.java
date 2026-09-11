package com.nova.bank.transaction.dto;

import com.nova.bank.transaction.enums.AccountStatus;
import com.nova.bank.transaction.enums.AccountType;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
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