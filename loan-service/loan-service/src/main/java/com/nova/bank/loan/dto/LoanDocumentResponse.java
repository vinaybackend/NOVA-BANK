package com.nova.bank.loan.dto;

import java.time.LocalDateTime;

public record LoanDocumentResponse(
        String id,
        String applicationId,
        String documentType,
        String originalFileName,
        boolean verified,
        String verifiedBy,
        LocalDateTime uploadedAt
) {
}
