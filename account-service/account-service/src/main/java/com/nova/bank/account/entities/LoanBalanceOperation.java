package com.nova.bank.account.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_balance_operations")
@Getter
@Setter
@NoArgsConstructor
public class LoanBalanceOperation {

    @Id
    @Column(length = 80)
    private String operationId;

    @Column(nullable = false, length = 30)
    private String customerId;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String operationType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
