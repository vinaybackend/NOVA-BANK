package com.nova.bank.dto;
import com.nova.bank.entities.KycDocument;
import com.nova.bank.entities.KycDocumentType;
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
public class KycResponse {

    private String kycId;

    private String customerId;

    private KycType kycType;

    private KycVerificationStatus kycVerificationStatus;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String reviewerId;

    private String reviewerName;

    private List<KycDocumentResponse> kycDocument;

    }