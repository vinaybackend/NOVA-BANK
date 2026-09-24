package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.dto.CreateAccountRequest;
import com.nova.bank.account.dto.UpdateAccountStatusRequest;
import com.nova.bank.account.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

// get account by accountId
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable String accountId) {

        AccountResponse response = accountService.getAccountById(accountId);

        return ResponseEntity.ok(response);
    }

    // get account by account number
    @GetMapping("/account-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {

        AccountResponse response = accountService.getAccountByNumber(accountNumber);

        return ResponseEntity.ok(response);
    }

    // get account detail by customerId
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable String customerId) {

        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/status/{accountId}")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable String accountId, @Valid @RequestBody UpdateAccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(accountId, request);

        return ResponseEntity.ok(response);
    }
}