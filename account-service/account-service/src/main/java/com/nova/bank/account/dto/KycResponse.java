package com.nova.bank.account.dto;

import com.nova.bank.account.entities.VerificationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycResponse {

    private String kycId;
    private String customerId;
    private VerificationStatus verificationStatus;
}
