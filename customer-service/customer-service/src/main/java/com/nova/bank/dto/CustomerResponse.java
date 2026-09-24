package com.nova.bank.dto;
import com.nova.bank.entities.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private String customerId;

    private Title title;

    private String firstName;

    private String middleName;

    private String lastName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String nationality;

    private MaritalStatus maritalStatus;

    private String email;

    private String mobileNumber;

    private String alternateMobileNumber;

    private String permanentAddress;

    private String permanentCity;

    private String permanentState;

    private String permanentCountry;

    private String permanentPinCode;

    private String communicationAddress;

    private String communicationCity;

    private String communicationState;

    private String communicationCountry;

    private String communicationPinCode;

    private String occupation;

    private BigDecimal annualIncome;

    private SourceOfIncome sourceOfIncome;

    private String language;

    private CustomerStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private KycResponse kycResponse;
}
