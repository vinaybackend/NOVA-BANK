package com.nova.bank.dto;
import com.nova.bank.entities.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(

        String customerId,

        Title title,

        String firstName,

        String middleName,

        String lastName,

        LocalDate dateOfBirth,

        Gender gender,

        String nationality,

        MaritalStatus maritalStatus,

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

        EmploymentType employmentType,

        String occupation,

        String employerName,

        BigDecimal annualIncome,

        SourceOfIncome sourceOfIncome,

        String language,

        CustomerStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
)
{
}