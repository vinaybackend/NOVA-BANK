package com.nova.bank.account.dto;

import com.nova.bank.account.clients.CustomerStatus;
import com.nova.bank.account.entities.KycVerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerEligibilityResponse {

    private String customerId;

    private CustomerStatus customerStatus;

    private KycVerificationStatus kycStatus;

    private boolean eligible;
}