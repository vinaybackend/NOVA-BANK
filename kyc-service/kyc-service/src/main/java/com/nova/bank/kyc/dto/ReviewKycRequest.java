package com.nova.bank.kyc.dto;

import jakarta.validation.constraints.NotBlank;

public class ReviewKycRequest {

    @NotBlank(message = "Reviewer ID is required")
    private String reviewerId;

    public String getReviewerId() {
        return reviewerId;
    }

    public ReviewKycRequest setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
        return this;
    }
}
