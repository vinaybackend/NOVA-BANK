package com.nova.bank.dto;

import com.nova.bank.entities.KycType;
import com.nova.bank.entities.KycVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycPendingResponse {

    private String kycId;

    private String customerId;

    private KycType kycType;

    private KycVerificationStatus kycVerificationStatus;

    private LocalDateTime verifiedAt;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
