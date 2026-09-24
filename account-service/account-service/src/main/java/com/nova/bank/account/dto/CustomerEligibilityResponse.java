package com.nova.bank.account.dto;

import com.nova.bank.account.entities.AccountStatus;
import com.nova.bank.account.entities.VerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerEligibilityResponse {

    private String customerId;

    private AccountStatus customerStatus;

    private VerificationStatus kycStatus;

    private boolean eligible;
}