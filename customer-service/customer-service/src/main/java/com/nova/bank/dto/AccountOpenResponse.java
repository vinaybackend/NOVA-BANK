package com.nova.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class AccountOpenResponse {
    private String customerId;
    private String accountNo;
    private String BranchName;
    private String about;

}
