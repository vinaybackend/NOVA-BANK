package com.nova.bank.loan.dto;

import com.nova.bank.loan.client.CustomerProfileResponse;
import com.nova.bank.loan.entity.LoanApplication;
import com.nova.bank.loan.entity.LoanApplicationHistory;

import java.util.List;

public record LoanApplicationReviewResponse(
        LoanApplication application,
        CustomerProfileResponse customer,
        List<LoanDocumentResponse> documents,
        List<LoanApplicationHistory> history
) {
}
