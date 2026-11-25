package com.nova.bank.loan.client;

public record CustomerResponse(
        String customerId,
        String name,
        String email,
        String mobileNumber,
        String status
) {
}
