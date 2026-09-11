package com.nova.bank.dto;

import com.nova.bank.entities.EmploymentType;
import com.nova.bank.entities.MaritalStatus;
import com.nova.bank.entities.SourceOfIncome;
import com.nova.bank.entities.Title;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateCustomerRequest(

        Title title,

        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Size(max = 50, message = "Middle name must not exceed 50 characters")
        String middleName,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Size(max = 100, message = "Nationality must not exceed 100 characters")
        String nationality,

        MaritalStatus maritalStatus,

        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Pattern(
                regexp = "^[0-9]{10,15}$",
                message = "Mobile number must contain 10 to 15 digits"
        )
        String mobileNumber,

        @Pattern(
                regexp = "^[0-9]{10,15}$",
                message = "Alternate mobile number must contain 10 to 15 digits"
        )
        String alternateMobileNumber,

        @Size(max = 150, message = "Permanent address must not exceed 150 characters")
        String permanentAddress,

        @Size(max = 100, message = "Permanent city must not exceed 100 characters")
        String permanentCity,

        @Size(max = 100, message = "Permanent state must not exceed 100 characters")
        String permanentState,

        @Size(max = 100, message = "Permanent country must not exceed 100 characters")
        String permanentCountry,

        @Pattern(
                regexp = "^[0-9]{4,10}$",
                message = "Invalid permanent PIN code"
        )
        String permanentPinCode,

        @Size(max = 150, message = "Communication address must not exceed 150 characters")
        String communicationAddress,

        @Size(max = 100, message = "Communication city must not exceed 100 characters")
        String communicationCity,

        @Size(max = 100, message = "Communication state must not exceed 100 characters")
        String communicationState,

        @Size(max = 100, message = "Communication country must not exceed 100 characters")
        String communicationCountry,

        @Pattern(
                regexp = "^[0-9]{4,10}$",
                message = "Invalid communication PIN code"
        )
        String communicationPinCode,

        EmploymentType employmentType,

        @Size(max = 100, message = "Occupation must not exceed 100 characters")
        String occupation,

        @Size(max = 150, message = "Employer name must not exceed 150 characters")
        String employerName,

        @DecimalMin(value = "0.0", inclusive = true, message = "Annual income cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "Invalid annual income")
        BigDecimal annualIncome,

        SourceOfIncome sourceOfIncome,

        @Size(max = 30, message = "Language must not exceed 30 characters")
        String language
) {
}