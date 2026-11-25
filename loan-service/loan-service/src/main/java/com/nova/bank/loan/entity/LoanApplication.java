package com.nova.bank.loan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 40)
    private String customerId;

    @Column(length = 100)
    private String customerName;

    @Column(length = 100)
    private String customerEmail;

    @Column(length = 30)
    private String customerPhone;

    @Column(length = 40)
    private String kycStatus;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LoanStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal existingMonthlyEmi = BigDecimal.ZERO;

    @Column(length = 100)
    private String employerName;

    @Column(length = 80)
    private String employmentType;

    @Column(length = 100)
    private String designation;

    private Integer workExperienceYears;

    @Column(length = 100)
    private String businessName;

    @Column(length = 100)
    private String businessType;

    private Integer businessVintageYears;

    @Column(length = 500)
    private String businessAddress;

    @Column(length = 30)
    private String businessRegistrationNumber;

    @Column(precision = 12, scale = 2)
    private BigDecimal annualTurnover;

    @Column(length = 100)
    private String assignedOfficerId;

    @Column(length = 20)
    private String recommendation;

    @Column(length = 2000)
    private String recommendationRemarks;

    @Column(precision = 12, scale = 2)
    private BigDecimal recommendedAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal recommendedInterestRate;

    private Integer recommendedTenureMonths;

    @Column(length = 2000)
    private String additionalDocumentsRequested;

    @Column(length = 2000)
    private String managerDecisionReason;

    private Integer creditScore;
    private BigDecimal assessedFoirPercent;
    private Boolean creditPassed;
    private Boolean offerAccepted;
    private Boolean agreementAccepted = false;
    private String agreementText;
    private String keyFactStatement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
