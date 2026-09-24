package com.nova.bank.dto;

import com.nova.bank.entities.KycDocumentType;
import com.nova.bank.entities.KycType;
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
    private KycDocumentType kycDocumentType;

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

    public KycDocumentType getDocumentType() {
        return kycDocumentType;
    }

    public CreateKycRequest setDocumentType(KycDocumentType kycDocumentType) {
        this.kycDocumentType = kycDocumentType;
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