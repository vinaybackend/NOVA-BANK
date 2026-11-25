package com.nova.bank.loan.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerProfileResponse(
        String customerId,
        String title,
        String name,
        String branchName,
        String branchCode,
        String ifscCode,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String maritalStatus,
        String email,
        String mobileNumber,
        String alternateMobileNumber,
        String permanentAddress,
        String permanentCity,
        String permanentState,
        String permanentCountry,
        String permanentPinCode,
        String communicationAddress,
        String communicationCity,
        String communicationState,
        String communicationCountry,
        String communicationPinCode,
        String occupation,
        BigDecimal annualIncome,
        String sourceOfIncome,
        String language,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal openingBalance,
        String profileImageUrl
) {
}
