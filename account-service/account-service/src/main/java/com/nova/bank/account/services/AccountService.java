package com.nova.bank.account.services;

import com.nova.bank.account.clients.KycClient;
import com.nova.bank.account.dto.AccountResponse;
import com.nova.bank.account.dto.CreateAccountRequest;
import com.nova.bank.account.dto.KycResponse;
import com.nova.bank.account.dto.UpdateAccountStatusRequest;
import com.nova.bank.account.entities.Account;
import com.nova.bank.account.entities.AccountStatus;
import com.nova.bank.account.entities.VerificationStatus;
import com.nova.bank.account.exceptions.*;
import com.nova.bank.account.repositories.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final KycClient kycClient;

    public AccountService(AccountRepository accountRepository, KycClient kycClient) {
        this.accountRepository = accountRepository;
        this.kycClient = kycClient;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        // Step 1: Check KYC
        KycResponse kyc = kycClient.getKycByCustomerId(request.getCustomerId());

        // Step 2: KYC must be approved
        if (kyc.getVerificationStatus() != VerificationStatus.APPROVED) {

            throw new KycNotApprovedException("Customer KYC is not approved");
        }

        // Step 3: Prevent duplicate account type
        if (accountRepository.existsByCustomerIdAndAccountType(request.getCustomerId(), request.getAccountType())) {

            throw new AccountAlreadyExistsException("Account already exists for customer: " + request.getCustomerId());
        }

        // Step 4: Create account
        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .accountId(generateAccountId())
                .accountNumber(generateAccountNumber())
                .customerId(request.getCustomerId())
                .accountType(request.getAccountType())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency().toUpperCase())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Step 5: Save
        Account savedAccount = accountRepository.save(account);

        // Step 6: Return response
        return mapToResponse(savedAccount);
    }

    private String generateAccountId() {

        return "ACC-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase();
    }

    private String generateAccountNumber() {

        long number = 1000000000L + (long) (Math.random() * 9000000000L);

        return String.valueOf(number);
    }

    private AccountResponse mapToResponse(Account account) {

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }


    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String accountId) {

        Account account = accountRepository.findByAccountId(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        return mapToResponse(account);
    }


    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(String customerId) {

        List<Account> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream().map(this::mapToResponse).toList();
    }

    // account status

    @Transactional
    public AccountResponse updateAccountStatus(String accountId, UpdateAccountStatusRequest request) {

        Account account = accountRepository.findByAccountId(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        AccountStatus currentStatus = account.getAccountStatus();
        AccountStatus newStatus = request.getAccountStatus();

        if (currentStatus == AccountStatus.CLOSED) {
            throw new IllegalStateException("Closed account cannot change status");
        }

        if (currentStatus == newStatus) {
            throw new IllegalStateException("Account is already " + newStatus);
        }

        account.setAccountStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());

        Account updatedAccount = accountRepository.save(account);

        return mapToResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse creditAccount(String accountNumber,BigDecimal amount){
        validateAmount(amount);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        validateAccountForTransaction(account);

        BigDecimal currentBalance = account.getBalance();
        BigDecimal newBalance= currentBalance.add(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount= accountRepository.save(account);
        return mapToResponse(updatedAccount);

    }

    @Transactional
    public AccountResponse debitAccount(String accountNumber,BigDecimal amount){
        validateAmount(amount);
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        validateAccountForTransaction(account);
        BigDecimal currentBalance = account.getBalance();

        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        BigDecimal newBalance = currentBalance.subtract(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount = accountRepository.save(account);

        return mapToResponse(updatedAccount);
    }

    // validate amount

    private void validateAmount(BigDecimal amount) {

        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
    }

    // validate account
    private void validateAccountForTransaction(Account account) {

        if (account.getAccountStatus() == AccountStatus.BLOCKED) {

            throw new AccountBlockedException("Account is blocked");
        }

        if (account.getAccountStatus() == AccountStatus.CLOSED) {

            throw new AccountClosedException("Account is closed");
        }

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {

            throw new IllegalStateException("Account is not active");
        }
    }
    }

