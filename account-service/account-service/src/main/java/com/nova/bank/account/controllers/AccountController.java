package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.dto.CreateAccountRequest;
import com.nova.bank.account.dto.CustomerTransferRequest;
import com.nova.bank.account.dto.CustomerTransferResponse;
import com.nova.bank.account.dto.UpdateAccountStatusRequest;
import com.nova.bank.account.dto.request.DepositRequest;
import com.nova.bank.account.entities.Account;
import com.nova.bank.account.repositories.AccountRepository;
import com.nova.bank.account.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    public AccountController(AccountService accountService, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/open-after-kyc-approval")
    public ResponseEntity<AccountResponse> openAccountAfterKycApproval(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.openAccountAfterKycApproval(request));
    }

// get account by accountId
    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER','ADMIN','TELLER')")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable String accountId) {

        AccountResponse accountById = accountService.getAccountById(accountId);

        return ResponseEntity.ok(accountById);
    }

    // get account by account number
    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','TELLER','ADMIN')")
    @GetMapping("/account-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        AccountResponse accountByNumber = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(accountByNumber
        );
    }

    // get all account details by customerId
    @PreAuthorize("hasAnyRole('CUSTOMER','LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable String customerId) {

        List<AccountResponse> accountsByCustomerId = accountService.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(accountsByCustomerId);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/{accountId}/debit-card-preview")
    public ResponseEntity<AccountResponse> issueDebitCardPreviewForCurrentCustomer(
            @PathVariable String accountId) {
        return ResponseEntity.ok(accountService.issueDebitCardPreviewForCurrentCustomer(accountId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<Page<AccountResponse>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(accountService.getAllAccounts(page, size));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/customer-transfer")
    public ResponseEntity<CustomerTransferResponse> transferForCurrentCustomer(@Valid @RequestBody CustomerTransferRequest request) {
        return ResponseEntity.ok(accountService.transferForCurrentCustomer(request));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @PatchMapping("/status/{accountId}")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable String accountId, @Valid @RequestBody UpdateAccountStatusRequest request) {

        AccountResponse response = accountService.updateAccountStatus(accountId, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all-account/{branchCode}")
    public ResponseEntity<Page<AccountResponse>> allAccountOfBranch(@PathVariable("branchCode") String branchCode,@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size){
        Page<AccountResponse> accountResponses = accountService.allBranchAccount(branchCode, page, size);
        return new ResponseEntity<>(accountResponses, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER','KYC_OFFICER','CUSTOMER')")
    @PatchMapping("/deposit/{accountNumber}")
    public ResponseEntity<String> depositIntoAccount(@PathVariable("accountNumber") String accountNumber, @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(accountService.depositIntoAc(accountNumber, request.getAmount()));
    }
}