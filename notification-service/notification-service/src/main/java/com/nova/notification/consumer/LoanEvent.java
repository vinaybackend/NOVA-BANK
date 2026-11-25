package com.nova.notification.consumer;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanEvent {
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

