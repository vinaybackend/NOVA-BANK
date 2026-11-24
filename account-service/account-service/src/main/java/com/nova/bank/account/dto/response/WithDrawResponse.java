package com.nova.bank.account.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithDrawResponse {
    String accountNumber;
    String accountType;
    String currency;
    String customerId;
    String name;
    String email;
    BigDecimal balance;

}
