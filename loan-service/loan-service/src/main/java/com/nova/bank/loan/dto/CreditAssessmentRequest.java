package com.nova.bank.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record CreditAssessmentRequest(@Min(300) @Max(900) int creditScore) {
}
