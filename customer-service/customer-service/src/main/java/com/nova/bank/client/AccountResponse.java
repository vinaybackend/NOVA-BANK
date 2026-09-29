package com.nova.bank.client;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class AccountResponse {

    private String accountId;
    private String accountNumber;
    private String customerId;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private BigDecimal balance;
    private String currency;
    private String branchId;
    private String branchName;
    private String ifsc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}