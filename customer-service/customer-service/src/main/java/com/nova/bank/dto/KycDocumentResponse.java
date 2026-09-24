package com.nova.bank.dto;

import com.nova.bank.entities.KycDocumentType;
import com.nova.bank.entities.KycDocumentVerificationStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentResponse {

    private String documentId;

    private String kycId;

    private KycDocumentType documentType;

    private String documentNumber;

    private String fileName;

    private String fileUrl;

    private KycDocumentVerificationStatus kycVerificationStatus;

    private LocalDateTime uploadedAt;

    private LocalDateTime verifiedAt;
}