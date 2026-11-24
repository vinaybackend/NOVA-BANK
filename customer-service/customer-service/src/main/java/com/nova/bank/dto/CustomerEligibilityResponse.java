package com.nova.bank.dto;

import com.nova.bank.entities.CustomerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerEligibilityResponse {

    private String customerId;

    private CustomerStatus customerStatus;

    private boolean eligible;
}