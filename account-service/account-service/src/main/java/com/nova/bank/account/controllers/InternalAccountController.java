package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.dto.LoanBalanceRequest;
import com.nova.bank.account.dto.LoanBalanceResponse;
import com.nova.bank.account.dto.response.WithDrawResponse;
import com.nova.bank.account.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts/internal")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyRole('TELLER','BRANCH_MANAGER','ADMIN')")
    @PostMapping("/credit/{accountNumber}")
    public ResponseEntity<AccountResponse> creditAccount(
            @PathVariable String accountNumber, @RequestParam BigDecimal amount, Authentication authentication) {

        AccountResponse response = accountService.creditAccount(accountNumber, amount);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('TELLER','BRANCH_MANAGER','ADMIN')")
    @PostMapping("/debit/{accountNumber}")
    public ResponseEntity<WithDrawResponse> debitAccount(
            @PathVariable String accountNumber, @RequestParam BigDecimal amount, Authentication authentication) {

        WithDrawResponse withDrawResponse = accountService.debitAccount(accountNumber, amount);

        return ResponseEntity.ok(withDrawResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    @PostMapping("/loan-disbursement")
    public ResponseEntity<LoanBalanceResponse> creditLoanDisbursement(
            @RequestBody @jakarta.validation.Valid LoanBalanceRequest request) {
        return ResponseEntity.ok(accountService.creditLoanDisbursement(request));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/loan-payment")
    public ResponseEntity<LoanBalanceResponse> debitLoanPayment(
            @RequestBody @jakarta.validation.Valid LoanBalanceRequest request) {
        return ResponseEntity.ok(accountService.debitLoanPayment(request));
    }

}