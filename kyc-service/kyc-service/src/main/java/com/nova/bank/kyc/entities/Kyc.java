package com.nova.bank.kyc.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_records", uniqueConstraints = @UniqueConstraint(name = "uk_kyc_kyc_id", columnNames = "kyc_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kyc_id", nullable = false, unique = true, length = 30, updatable = false)
    private String kycId;

    @Column(name = "customer_id", nullable = false, length = 30)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false, length = 20)
    private KycType kycType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private KycDocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private KycVerificationStatus verificationStatus = KycVerificationStatus.DRAFT;

    @Column(name = "reviewer_id", length = 50)
    private String reviewerId;

    @Column(name = "reviewer_name")
    private String reviewerName;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


//     Optimistic locking.
//     Prevents two employees from updating the same KYC
//     record at the same time without detection.
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (verificationStatus == null) {
            verificationStatus = KycVerificationStatus.DRAFT;
        }

        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

