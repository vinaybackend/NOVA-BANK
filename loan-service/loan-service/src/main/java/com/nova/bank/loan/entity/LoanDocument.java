package com.nova.bank.loan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_documents")
@Getter
@Setter
@NoArgsConstructor
public class LoanDocument {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String applicationId;

    @Column(nullable = false, length = 100)
    private String documentType;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 1000)
    private String filePath;

    @Column(nullable = false)
    private boolean verified;

    @Column(length = 100)
    private String verifiedBy;

    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
