package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotBlank;

public record LoanAssignmentRequest(@NotBlank String officerId) {
}
