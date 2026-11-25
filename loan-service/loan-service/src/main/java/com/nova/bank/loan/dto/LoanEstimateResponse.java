package com.nova.bank.loan.dto;

import java.math.BigDecimal;

public record LoanEstimateResponse(
        BigDecimal monthlyEmi,
        BigDecimal estimatedFoirPercent,
        BigDecimal maximumEligibleEmi,
        boolean eligible
) {
}
