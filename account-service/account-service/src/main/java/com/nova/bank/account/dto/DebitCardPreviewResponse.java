package com.nova.bank.account.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class DebitCardPreviewResponse {
    String cardStatus;
    String cardType;
    String lastFourDigits;
    LocalDateTime issuedAt;
    LocalDate expiryDate;
    String debitCardReference;

}
