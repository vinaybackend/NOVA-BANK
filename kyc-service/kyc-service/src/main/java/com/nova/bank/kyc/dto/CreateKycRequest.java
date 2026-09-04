package com.nova.bank.kyc.dto;

import com.nova.bank.kyc.entities.DocumentType;
import com.nova.bank.kyc.entities.KycType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateKycRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 30, message = "Customer ID must not exceed 30 characters")
    private String customerId;

    @NotNull(message = "KYC type is required")
    private KycType kycType;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber;

    public String getCustomerId() {
        return customerId;
    }

    public CreateKycRequest setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public KycType getKycType() {
        return kycType;
    }

    public CreateKycRequest setKycType(KycType kycType) {
        this.kycType = kycType;
        return this;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public CreateKycRequest setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public CreateKycRequest setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }
}