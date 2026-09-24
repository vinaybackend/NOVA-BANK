package com.nova.bank.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true, length = 30, updatable = false)
    private String documentId;

    @Column(name = "kyc_id", nullable = false, length = 30, updatable = false)
    private String kycId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private KycDocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    // NEW
    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "document_hash", length = 128)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 30)
    private KycDocumentVerificationStatus verificationStatus;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {

        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }

        if (verificationStatus == null) {
            verificationStatus =
                    KycDocumentVerificationStatus.PENDING;
        }
    }
}