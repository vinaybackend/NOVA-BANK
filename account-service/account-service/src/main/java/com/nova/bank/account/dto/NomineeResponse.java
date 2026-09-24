package com.nova.bank.account.dto;
import com.nova.bank.account.entities.NomineeRelationship;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class NomineeResponse {

    private String nomineeId;

    private String accountId;

    private String fullName;

    private NomineeRelationship relationship;

    private LocalDate dateOfBirth;

    private String mobileNumber;

    private String address;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
