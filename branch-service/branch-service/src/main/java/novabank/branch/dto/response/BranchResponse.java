package novabank.branch.dto.response;

import novabank.branch.entities.BranchStatus;
import novabank.branch.entities.BranchType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record BranchResponse(
        Long id,
        String branchCode,
        String branchName,
        String ifscCode,
        BranchType branchType,
        String address,
        String city,
        String district,
        String state,
        String pinCode,
        String country,
        String phone,
        String email,
        BranchStatus status,
        LocalDate openingDate,
        LocalTime openingTime,
        LocalTime closingTime,
        String timeZone,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}

