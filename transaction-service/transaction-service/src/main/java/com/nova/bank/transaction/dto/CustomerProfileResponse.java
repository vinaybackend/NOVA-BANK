package com.nova.bank.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class CustomerProfileResponse {
    private String customerId;
    private String name;
    private String email;

}
