package com.nova.bank.loan.dto;

import jakarta.validation.constraints.NotBlank;

public record AdditionalDocumentsRequest(@NotBlank String requestedDocuments) {
}
