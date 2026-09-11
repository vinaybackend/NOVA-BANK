package com.nova.bank.transaction.clients;

import com.nova.bank.transaction.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/account-number/{accountNumber}")
    AccountResponse getAccountByNumber(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/v1/accounts/internal/credit/{accountNumber}")
    AccountResponse creditAccount(@PathVariable("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/v1/accounts/internal/debit/{accountNumber}")
    AccountResponse debitAccount(@PathVariable("accountNumber") String accountNumber, @RequestParam("amount") BigDecimal amount);
}