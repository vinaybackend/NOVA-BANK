package com.nova.bank.dto.request;

import com.nova.bank.entities.SourceOfIncome;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerUpdatedRequest {

    // All fields OPTIONAL — only validated when present.
    // Pattern/Size constraints are skipped by Bean Validation when value is null.

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must contain 10 digits")
    private String mobileNumber;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Alternate mobile number must contain 10 digits")
    private String alternateMobileNumber;

    @Size(max = 150, message = "Permanent address must not exceed 150 characters")
    private String permanentAddress;

    @Size(max = 100, message = "Permanent city must not exceed 100 characters")
    private String permanentCity;

    @Size(max = 100, message = "Permanent state must not exceed 100 characters")
    private String permanentState;

    @Size(max = 100, message = "Permanent country must not exceed 100 characters")
    private String permanentCountry;

    @Pattern(regexp = "^[0-9]{4,10}$", message = "Invalid permanent PIN code")
    private String permanentPinCode;

    @Size(max = 150, message = "Communication address must not exceed 150 characters")
    private String communicationAddress;

    @Size(max = 100, message = "Communication city must not exceed 100 characters")
    private String communicationCity;

    @Size(max = 100, message = "Communication state must not exceed 100 characters")
    private String communicationState;

    @Size(max = 100, message = "Communication country must not exceed 100 characters")
    private String communicationCountry;

    @Pattern(regexp = "^$|^[0-9]{4,10}$", message = "Invalid communication PIN code")
    private String communicationPinCode;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @DecimalMin(value = "0.0", inclusive = true, message = "Annual income cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Invalid annual income")
    private BigDecimal annualIncome;

    // Enum: @NotNull removed — if null, service will keep existing value.
    private SourceOfIncome sourceOfIncome;

    @Size(max = 30, message = "Language must not exceed 30 characters")
    private String language;
}