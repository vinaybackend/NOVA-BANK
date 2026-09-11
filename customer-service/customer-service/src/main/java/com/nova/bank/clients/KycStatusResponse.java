package com.nova.bank.clients;

public record KycStatusResponse(
        String customerId,
        String verificationStatus
) {
}