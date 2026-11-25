package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record EmploymentInfoRequest(
        @NotBlank String employerName,
        @NotBlank String employmentType,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monthlyIncome,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal existingMonthlyEmi,
        String designation,
        Integer workExperienceYears,
        String businessName,
        String businessType,
        Integer businessVintageYears,
        String businessAddress,
        String businessRegistrationNumber,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal annualTurnover
) {
}
