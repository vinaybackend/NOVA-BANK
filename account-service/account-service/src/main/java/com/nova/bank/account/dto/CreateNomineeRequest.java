package com.nova.bank.account.dto;

import com.nova.bank.account.entities.NomineeRelationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateNomineeRequest {

    @NotBlank(message = "Nominee full name is required")
    @Size(max = 150, message = "Nominee full name must not exceed 150 characters")
    private String fullName;

    @NotNull(message = "Nominee relationship is required")
    private NomineeRelationship relationship;

    @Past(message = "Nominee date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Nominee mobile number must contain 10 to 15 digits")
    private String mobileNumber;

    @Size(max = 250, message = "Nominee address must not exceed 250 characters")
    private String address;
}