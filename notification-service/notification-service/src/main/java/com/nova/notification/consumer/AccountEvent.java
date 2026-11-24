package com.nova.notification.consumer;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountEvent {
    String eventId;
    String accountNumber;
    String email;
    String name;
    BigDecimal balance;
    LocalDateTime createdAt;
    String branchName;
    String ifsc;
    String debitCardNumber;
    String username;
    String password;
}
