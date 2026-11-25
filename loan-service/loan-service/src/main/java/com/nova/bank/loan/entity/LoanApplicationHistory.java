package com.nova.bank.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_application_history")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplicationHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String applicationId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 100)
    private String actorId;

    @Column(length = 40)
    private String previousStatus;

    @Column(length = 40)
    private String currentStatus;

    @Column(length = 2000)
    private String remarks;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
