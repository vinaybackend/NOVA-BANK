package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.dto.CreateAccountRequest;
import com.nova.bank.account.dto.UpdateAccountStatusRequest;
import com.nova.bank.account.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER')")
    @PostMapping("/create")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        return response;
    }

// get account by accountId
    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER')")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable String accountId) {

        AccountResponse response = accountService.getAccountById(accountId);

        return ResponseEntity.ok(response);
    }

    // get account by account number
    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','TELLER')")
    @GetMapping("/account-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {

        AccountResponse response = accountService.getAccountByNumber(accountNumber);

        return ResponseEntity.ok(response);
    }

    // get account detail by customerId
    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','ADMIN')")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<AccountResponse> getAccountsByCustomerId(@PathVariable String customerId) {

        AccountResponse accountsByCustomerId = accountService.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(accountsByCustomerId);
    }

    @PatchMapping("/status/{accountId}")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable String accountId, @Valid @RequestBody UpdateAccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(accountId, request);

        return ResponseEntity.ok(response);
    }
}