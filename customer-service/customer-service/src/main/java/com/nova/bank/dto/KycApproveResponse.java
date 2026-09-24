package com.nova.bank.dto;
import com.nova.bank.entities.KycType;
import com.nova.bank.entities.KycVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycApproveResponse {

    private String kycId;

    private String customerId;

    private KycType kycType;

//    private KycDocumentType kycDocumentType;

//    private String documentNumber;

    private KycVerificationStatus kycVerificationStatus;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String reviewerId;

    private String reviewerName;

    private List<KycDocumentResponse> kycDocument;
    private CustomerResponse customerResponse;

    }