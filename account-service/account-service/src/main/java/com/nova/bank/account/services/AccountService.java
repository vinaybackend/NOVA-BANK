package com.nova.bank.account.services;

import com.nova.bank.account.clients.CustomerClient;
import com.nova.bank.account.dto.*;
import com.nova.bank.account.entities.*;
import com.nova.bank.account.exceptions.*;
import com.nova.bank.account.repositories.AccountHolderRepository;
import com.nova.bank.account.repositories.AccountNomineeRepository;
import com.nova.bank.account.repositories.AccountRepository;
import com.nova.bank.account.repositories.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final CustomerClient customerClient;
    private final BranchRepository branchRepository;
    private final AccountHolderRepository accountHolderRepository;
    private final AccountRepository accountRepository;
    private final AccountNomineeRepository accountNomineeRepository;

    public AccountService(CustomerClient customerClient, BranchRepository branchRepository, AccountHolderRepository accountHolderRepository, AccountRepository accountRepository, AccountNomineeRepository accountNomineeRepository) {
        this.customerClient = customerClient;
        this.branchRepository = branchRepository;
        this.accountHolderRepository = accountHolderRepository;
        this.accountRepository = accountRepository;
        this.accountNomineeRepository = accountNomineeRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        //Check customer + KYC eligibility
        CustomerEligibilityResponse eligibility = customerClient.getAccountEligibility(request.getCustomerId());

        if (!eligibility.isEligible()) {

            throw new IllegalStateException("Customer is not eligible for account opening. " + "Customer status: " + eligibility.getCustomerStatus() + ", KYC status: " + eligibility.getKycStatus());
        }

        // Check whether account already exists
        if (accountRepository.existsByCustomerIdAndAccountType(request.getCustomerId(), request.getAccountType())) {

            throw new AccountAlreadyExistsException("Account already exists for customer: " + request.getCustomerId() + " with type: " + request.getAccountType());
        }

        // Validate branch
        Branch branch = branchRepository.findByBranchId(request.getBranchId()).orElseThrow(() -> new IllegalArgumentException("Branch not found: " + request.getBranchId()));

        // Branch must be ACTIVE
        if (branch.getStatus() != BranchStatus.ACTIVE) {

            throw new IllegalStateException("Account cannot be opened because branch is " + branch.getStatus());
        }

        // Generate identifiers
        String accountId = generateAccountId();
        String accountNumber = generateAccountNumber();

        LocalDateTime now = LocalDateTime.now();

        //Create Account
        Account account = Account.builder()
                .accountId(accountId)
                .accountNumber(accountNumber)
                .customerId(request.getCustomerId())
                .branchId(branch.getBranchId())
                .branchName(branch.getBranchName())
                .ifsc(branch.getIfsc())
                .accountType(request.getAccountType())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency(request.getCurrency().toUpperCase())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account savedAccount = accountRepository.save(account);

        // 7. Create PRIMARY Account Holder
        AccountHolder primaryHolder = AccountHolder.builder()
                .accountId(savedAccount.getAccountId())
                .customerId(savedAccount.getCustomerId())
                .holderType(AccountHolderType.PRIMARY)
                .addedAt(now)
                .build();

        accountHolderRepository.save(primaryHolder);

        // 8. Create Nominee
        if (request.getNominee() != null) {

            CreateNomineeRequest nomineeRequest = request.getNominee();

            AccountNominee nominee = AccountNominee.builder()
                    .nomineeId(generateNomineeId())
                    .accountId(savedAccount.getAccountId())
                    .fullName(nomineeRequest.getFullName())
                    .relationship(nomineeRequest.getRelationship())
                    .dateOfBirth(nomineeRequest.getDateOfBirth())
                    .mobileNumber(nomineeRequest.getMobileNumber())
                    .address(nomineeRequest.getAddress())
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            accountNomineeRepository.save(nominee);
        }

        // 9. Return Account response
        return mapToResponse(savedAccount);
    }
    private String generateNomineeId() {

        return "NOM-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase();
    }
    private AccountResponse mapToResponse(Account account) {

        AccountNominee nominee = accountNomineeRepository.findByAccountIdAndActiveTrue(account.getAccountId()).orElse(null);

        NomineeResponse nomineeResponse = null;

        if (nominee != null) {

            nomineeResponse = NomineeResponse.builder()
                    .nomineeId(nominee.getNomineeId())
                    .accountId(nominee.getAccountId())
                    .fullName(nominee.getFullName())
                    .relationship(nominee.getRelationship())
                    .dateOfBirth(nominee.getDateOfBirth())
                    .mobileNumber(nominee.getMobileNumber())
                    .address(nominee.getAddress())
                    .active(nominee.getActive())
                    .createdAt(nominee.getCreatedAt())
                    .updatedAt(nominee.getUpdatedAt())
                    .build();
        }

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .balance(account.getBalance())
                .branchId(account.getBranchId())
                .branchName(account.getBranchName())
                .ifsc(account.getIfsc())
                .currency(account.getCurrency())
                .nominee(nomineeResponse)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
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

        long number = 1000000000L
                + (long) (Math.random() * 9000000000L);

        return String.valueOf(number);
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

