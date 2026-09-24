package com.nova.bank.dto;

import com.nova.bank.entities.CustomerStatus;
import com.nova.bank.entities.KycVerificationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerEligibilityResponse {

    private String customerId;

    private CustomerStatus customerStatus;

    private KycVerificationStatus kycStatus;

    private boolean eligible;
}