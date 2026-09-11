package com.nova.bank.kyc.dto;

import com.nova.bank.kyc.entities.KycDocumentType;
import com.nova.bank.kyc.entities.KycType;
import com.nova.bank.kyc.entities.KycVerificationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public class KycResponse {

    private String kycId;

    private String customerId;

    private KycType kycType;

    private KycDocumentType kycDocumentType;

    private String documentNumber;

    private KycVerificationStatus kycVerificationStatus;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public String getReviewerId() {
        return reviewerId;
    }

    public KycResponse setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
        return this;
    }

    private String reviewerId;

    public String getReviewerName() {
        return reviewerName;
    }

    public KycResponse setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
        return this;
    }

    private String reviewerName;

    public String getKycId() {
        return kycId;
    }

    public KycResponse setKycId(String kycId) {
        this.kycId = kycId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public KycResponse setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public KycType getKycType() {
        return kycType;
    }

    public KycResponse setKycType(KycType kycType) {
        this.kycType = kycType;
        return this;
    }

    public KycDocumentType getKycDocumentType() {
        return kycDocumentType;
    }

    public KycResponse setKycDocumentType(KycDocumentType kycDocumentType) {
        this.kycDocumentType = kycDocumentType;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public KycResponse setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public KycVerificationStatus getKycVerificationStatus() {
        return kycVerificationStatus;
    }

    public KycResponse setKycVerificationStatus(KycVerificationStatus kycVerificationStatus) {
        this.kycVerificationStatus = kycVerificationStatus;
        return this;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public KycResponse setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
        return this;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public KycResponse setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public KycResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public KycResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}