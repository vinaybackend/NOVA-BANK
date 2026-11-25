package com.nova.bank.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomerProfileResponse {
    private String customerId;
    private String name;
    private String email;
    private String mobileNumber;
}
