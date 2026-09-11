package com.nova.bank.kyc.dto;

public record KycStatusResponse(
        String customerId,
        String verificationStatus
) {
}