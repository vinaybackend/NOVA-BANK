package com.nova.bank.loan.producer;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanNotificationEvent {
    private String eventId;
    private String eventType;
    private String applicationId;
    private String loanAccountId;
    private String customerId;
    private String customerName;
    private String email;
    private BigDecimal amount;
    private String status;
    private LocalDate dueDate;
    private String message;
}

