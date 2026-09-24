package com.nova.bank.dto;

public record KycStatusResponse(
        String customerId,
        String verificationStatus
) {
}