package com.nova.bank.dto;
import com.nova.bank.entities.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOnboardingRequest {

    @NotNull(message = "Title is required")
    private Title title;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Middle name must not exceed 50 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Nationality is required")
    @Size(max = 50, message = "Nationality must not exceed 50 characters")
    private String nationality;

    private MaritalStatus maritalStatus;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Mobile number must contain 10 to 15 digits"
    )
    private String mobileNumber;

    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Alternate mobile number must contain 10 to 15 digits"
    )
    private String alternateMobileNumber;

    @NotBlank(message = "Permanent address is required")
    @Size(max = 150, message = "Permanent address must not exceed 150 characters")
    private String permanentAddress;

    @NotBlank(message = "Permanent city is required")
    @Size(max = 100, message = "Permanent city must not exceed 100 characters")
    private String permanentCity;

    @NotBlank(message = "Permanent state is required")
    @Size(max = 100, message = "Permanent state must not exceed 100 characters")
    private String permanentState;

    @NotBlank(message = "Permanent country is required")
    @Size(max = 100, message = "Permanent country must not exceed 100 characters")
    private String permanentCountry;

    @NotBlank(message = "Permanent PIN code is required")
    @Pattern(
            regexp = "^[0-9]{4,10}$",
            message = "Invalid permanent PIN code"
    )
    private String permanentPinCode;

    @Size(max = 150, message = "Communication address must not exceed 150 characters")
    private String communicationAddress;

    @Size(max = 100, message = "Communication city must not exceed 100 characters")
    private String communicationCity;

    @Size(max = 100, message = "Communication state must not exceed 100 characters")
    private String communicationState;

    @Size(max = 100, message = "Communication country must not exceed 100 characters")
    private String communicationCountry;

    @Pattern(
            regexp = "^[0-9]{4,10}$",
            message = "Invalid communication PIN code"
    )
    private String communicationPinCode;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Annual income cannot be negative"
    )
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Invalid annual income"
    )
    private BigDecimal annualIncome;

    private SourceOfIncome sourceOfIncome;

    @Size(max = 30, message = "Language must not exceed 30 characters")
    private String language;

    @Valid
    @NotNull(message = "KYC information is required")
    private KycRequest kyc;

}