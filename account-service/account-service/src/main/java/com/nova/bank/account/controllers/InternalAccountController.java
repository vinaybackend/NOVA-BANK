package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts/internal")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/credit/{accountNumber}")
    public ResponseEntity<AccountResponse> creditAccount(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {

        AccountResponse response = accountService.creditAccount(accountNumber, amount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit/{accountNumber}")
    public ResponseEntity<AccountResponse> debitAccount(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {

        AccountResponse response = accountService.debitAccount(accountNumber, amount);

        return ResponseEntity.ok(response);
    }
}