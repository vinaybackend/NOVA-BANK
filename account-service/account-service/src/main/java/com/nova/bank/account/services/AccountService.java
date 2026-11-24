package com.nova.bank.account.services;

import com.nova.bank.account.clients.AuthClient;
import com.nova.bank.account.clients.AuthRegistrationRequest;
import com.nova.bank.account.clients.BranchClient;
import com.nova.bank.account.clients.CustomerClient;
import com.nova.bank.account.clients.branch.BranchResponse;
import com.nova.bank.account.clients.branch.BranchStatus;
import com.nova.bank.account.dto.*;
import com.nova.bank.account.dto.response.WithDrawResponse;
import com.nova.bank.account.entities.*;
import com.nova.bank.account.exceptions.*;
import com.nova.bank.account.producer.AccountEvenPublisher;
import com.nova.bank.account.producer.AccountEvent;
import com.nova.bank.account.producer.LoanTransactionEvent;
import com.nova.bank.account.producer.LoanTransactionEventPublisher;
import com.nova.bank.account.repositories.AccountRepository;
import com.nova.bank.account.repositories.LoanBalanceOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final CustomerClient customerClient;
    private final AccountRepository accountRepository;
    private final BranchClient branchClient;
    private final AccountEvenPublisher accountEvenPublisher;
    private final AuthClient authClient;
    private final LoanBalanceOperationRepository loanBalanceOperationRepository;
    private final LoanTransactionEventPublisher loanTransactionEventPublisher;


    public AccountService(CustomerClient customerClient, AccountRepository accountRepository, BranchClient branchClient, AccountEvenPublisher accountEvenPublisher, AuthClient authClient, LoanBalanceOperationRepository loanBalanceOperationRepository, LoanTransactionEventPublisher loanTransactionEventPublisher) {
        this.customerClient = customerClient;
        this.accountRepository = accountRepository;
        this.branchClient = branchClient;
        this.accountEvenPublisher = accountEvenPublisher;
        this.authClient = authClient;
        this.loanBalanceOperationRepository = loanBalanceOperationRepository;
        this.loanTransactionEventPublisher = loanTransactionEventPublisher;
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
        BranchResponse branch = branchClient.getBranchById(request.getBranchCode());
        if (branch == null) {
            throw new IllegalArgumentException("Branch code is null: " + request.getBranchCode());
        }



        // Branch must be ACTIVE
        if (branch.status() != BranchStatus.ACTIVE) {
            throw new IllegalStateException("Account cannot be opened because branch is " + branch.status());
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
                .branchCode(branch.branchCode())
                .branchName(branch.branchName())
                .ifsc(branch.ifscCode())
                .accountType(request.getAccountType())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(request.getBalance())
                .currency(request.getCurrency().toUpperCase())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account savedAccount = accountRepository.save(account);
        CustomerProfileResponse myProfile = customerClient.getMyProfile();

        //kafka
        AccountEvent accountEvent = new AccountEvent();
        accountEvent.setAccountNumber(savedAccount.getAccountNumber());
        accountEvent.setCreatedAt(savedAccount.getCreatedAt());
        accountEvent.setEventId(UUID.randomUUID().toString().substring(0,8));
        accountEvent.setIfsc(savedAccount.getIfsc());
        accountEvent.setEmail(myProfile.getEmail());
        accountEvent.setBranchName(savedAccount.getBranchName());
        accountEvent.setBalance(savedAccount.getBalance());
        accountEvent.setDebitCardNumber(savedAccount.getDebitCardNumber());
        accountEvent.setName(myProfile.getName());
        accountEvenPublisher.accountEventCreated(accountEvent);
        return mapToResponse(savedAccount);
    }

    @Transactional
    public AccountResponse openAccountAfterKycApproval(CreateAccountRequest request) {
        return accountRepository.findByCustomerIdAndAccountType(request.getCustomerId(), request.getAccountType())
                .map(this::mapToResponse)
                .orElseGet(() -> createAccountWithoutEligibilityLookup(request));
    }

    private AccountResponse createAccountWithoutEligibilityLookup(CreateAccountRequest request) {
        BranchResponse branch = branchClient.getBranchById(request.getBranchCode());
        if (branch.status() != BranchStatus.ACTIVE) {
            throw new IllegalStateException("Account cannot be opened because branch is " + branch.status());
        }

        LocalDateTime now = LocalDateTime.now();
        Account account = Account.builder()
                .accountId(generateAccountId())
                .accountNumber(generateAccountNumber())
                .name(request.getName())
                .email(request.getEmail())
                .customerId(request.getCustomerId())
                .branchCode(branch.branchCode())
                .branchName(branch.branchName())
                .ifsc(branch.ifscCode())
                .accountType(request.getAccountType())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(request.getBalance())
                .currency(request.getCurrency().toUpperCase())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account savedAccount = accountRepository.save(account);



        String generatedPassword = generateSecurePassword();


        AuthRegistrationRequest authReq = new AuthRegistrationRequest();
        authReq.setName(request.getName());
        authReq.setEmail(request.getEmail());
        authReq.setPhone(request.getMobileNumber());
        authReq.setPassword(generatedPassword);
        authReq.setRoleName("CUSTOMER");
        authReq.setBranchName(branch.branchName());
        authReq.setIfsc(branch.ifscCode());
        authReq.setBranchCode(branch.branchCode());
        String authUserId;
        try {
            // Auth Service returns the generated User ID as a String
            authUserId = authClient.registerEmployeeAuth(authReq);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create auth account: " + e.getMessage(), e);
        }


        AccountEvent accountEvent = new AccountEvent();
        accountEvent.setAccountNumber(savedAccount.getAccountNumber());
        accountEvent.setCreatedAt(savedAccount.getCreatedAt());
        accountEvent.setEventId(UUID.randomUUID().toString().substring(0,8));
        accountEvent.setIfsc(savedAccount.getIfsc());
        accountEvent.setEmail(request.getEmail());
        accountEvent.setBranchName(savedAccount.getBranchName());
        accountEvent.setBalance(savedAccount.getBalance());
        accountEvent.setDebitCardNumber(savedAccount.getDebitCardNumber());
        accountEvent.setName(request.getName());
        accountEvent.setUsername(request.getEmail());
        accountEvent.setPassword(generatedPassword);
        accountEvenPublisher.accountEventCreated(accountEvent);
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

        DebitCardPreviewResponse debitCard = account.getDebitCardStatus() == null ? null : DebitCardPreviewResponse.builder()
                        .cardStatus(account.getDebitCardStatus())
                        .debitCardReference(account.getDebitCardNumber())
                        .cardType(account.getDebitCardType())
                        .lastFourDigits(account.getDebitCardLastFourDigits())
                        .issuedAt(account.getDebitCardIssuedAt())
                        .expiryDate(account.getDebitCardExpiryDate())
                        .build();

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .balance(account.getBalance())
                .branchCode(account.getBranchCode())
                .branchName(account.getBranchName())
                .ifsc(account.getIfsc())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .debitCard(debitCard)
                .name(account.getName())
                .email(account.getEmail())
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
    public AccountResponse getAccountByIdForCurrentCustomer(String accountId) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        if (!account.getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("You can only view your own accounts");
        }
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByIdForBranch(String accountId, String branchCode) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        validateAccountBranch(account, branchCode);
        return mapToResponse(account);
    }


    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {

        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumberForCurrentCustomer(String accountNumber) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        if (!account.getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("You can only view your own accounts");
        }
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumberForBranch(String accountNumber, String branchCode) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        validateAccountBranch(account, branchCode);
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public java.util.List<AccountResponse> getAccountsByCustomerIdForBranch(String customerId, String branchCode) {
        return accountRepository.findByCustomerIdAndBranchCode(customerId, branchCode)
                .stream().map(this::mapToResponse).toList();
    }

    private void validateAccountBranch(Account account, String branchCode) {
        if (branchCode == null || branchCode.isBlank() || !account.getBranchCode().equals(branchCode)) {
            throw new AccessDeniedException("You can only access accounts at your assigned branch");
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<AccountResponse> getAccountsByCustomerId(String customerId) {

        java.util.List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return accountRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public java.util.List<String> getCustomerIdsByBranchId(String branchCode) {
        return accountRepository.findByBranchCode(branchCode).stream()
                .map(Account::getCustomerId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<AccountResponse> getAccountsByCustomerIdForCurrentCustomer(String customerId) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        if (!customer.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You can only view your own accounts");
        }
        return getAccountsByCustomerId(customerId);
    }

    @Transactional
    public AccountResponse issueDebitCardPreviewForCurrentCustomer(String accountId) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        if (!account.getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("You can only issue a card preview for your own account");
        }
        account.issueDebitCardPreviewIfMissing();
        return mapToResponse(accountRepository.save(account));
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
    public AccountResponse creditAccount(String accountNumber, BigDecimal amount) {
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
    public LoanBalanceResponse creditLoanDisbursement(LoanBalanceRequest request) {
        Account account;
        LoanBalanceOperation existing = loanBalanceOperationRepository.findById(request.operationId())
                .orElse(null);
        if (existing != null) {
            account = accountRepository.findByAccountNumberForUpdate(existing.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            validateSameLoanOperation(existing, request, "DISBURSEMENT", account);
            return loanBalanceResponse(account);
        }

        account = request.accountNumber() == null || request.accountNumber().isBlank()
                ? findCustomerAccountForUpdate(request.customerId())
                : accountRepository.findByAccountNumberForUpdate(request.accountNumber())
                        .orElseThrow(() -> new AccountNotFoundException(
                                "Beneficiary account not found: " + request.accountNumber()));
        if (!account.getCustomerId().equals(request.customerId())) {
            throw new IllegalArgumentException("Beneficiary account does not belong to the customer");
        }
        validateAccountForTransaction(account);
        LoanBalanceOperation operation = createLoanOperation(request, "DISBURSEMENT", account);
        account.setBalance(account.getBalance().add(request.amount()));
        account.setUpdatedAt(LocalDateTime.now());
        loanBalanceOperationRepository.save(operation);
        publishLoanTransactionEvent(account, request.amount(), "DISBURSEMENT");
        return loanBalanceResponse(accountRepository.save(account));
    }

    @Transactional
    public LoanBalanceResponse debitLoanPayment(LoanBalanceRequest request) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        if (!customer.getCustomerId().equals(request.customerId())) {
            throw new AccessDeniedException("Loan payment can only use the signed-in customer's account");
        }

        Account account = accountRepository.findByAccountNumberForUpdate(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (!account.getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Loan payment account does not belong to the signed-in customer");
        }

        LoanBalanceOperation existing = loanBalanceOperationRepository.findById(request.operationId())
                .orElse(null);
        if (existing != null) {
            validateSameLoanOperation(existing, request, "PAYMENT", account);
            return loanBalanceResponse(account);
        }
        validateAccountForTransaction(account);
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for loan payment");
        }

        LoanBalanceOperation operation = createLoanOperation(request, "PAYMENT", account);
        account.setBalance(account.getBalance().subtract(request.amount()));
        account.setUpdatedAt(LocalDateTime.now());
        loanBalanceOperationRepository.save(operation);
        publishLoanTransactionEvent(account, request.amount().negate(), "PAYMENT");
        return loanBalanceResponse(accountRepository.save(account));
    }

    private Account findCustomerAccountForUpdate(String customerId) {
        for (AccountType accountType : new AccountType[]{AccountType.SAVINGS, AccountType.CURRENT}) {
            List<Account> accounts = accountRepository.findActiveAccountsForUpdate(
                    customerId, accountType, AccountStatus.ACTIVE);
            if (!accounts.isEmpty()) {
                return accounts.get(0);
            }
        }
        throw new AccountNotFoundException("Customer does not have an active savings or current account");
    }

    private LoanBalanceOperation createLoanOperation(LoanBalanceRequest request,
                                                      String operationType,
                                                      Account account) {
        LoanBalanceOperation operation = new LoanBalanceOperation();
        operation.setOperationId(request.operationId());
        operation.setCustomerId(request.customerId());
        operation.setAccountNumber(account.getAccountNumber());
        operation.setOperationType(operationType);
        operation.setAmount(request.amount());
        return operation;
    }

    private void validateSameLoanOperation(LoanBalanceOperation existing,
                                           LoanBalanceRequest request,
                                           String operationType,
                                           Account account) {
        if (!existing.getCustomerId().equals(request.customerId())
                || !existing.getAccountNumber().equals(account.getAccountNumber())
                || (request.accountNumber() != null
                    && !request.accountNumber().equals(existing.getAccountNumber()))
                || !existing.getOperationType().equals(operationType)
                || existing.getAmount().compareTo(request.amount()) != 0) {
            throw new IllegalArgumentException("Operation ID was already used for a different loan balance change");
        }
    }

    private LoanBalanceResponse loanBalanceResponse(Account account) {
        return new LoanBalanceResponse(account.getAccountNumber(), account.getCustomerId(),
                account.getCurrency(), account.getBalance());
    }

    @Transactional
    public WithDrawResponse debitAccount(String accountNumber, BigDecimal amount) {
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
        WithDrawResponse withDrawResponse = new WithDrawResponse();
        withDrawResponse.setAccountNumber(updatedAccount.getAccountNumber());
        withDrawResponse.setAccountType(updatedAccount.getAccountType().toString());
        withDrawResponse.setCurrency(updatedAccount.getCurrency());
        withDrawResponse.setCustomerId(updatedAccount.getCustomerId());
        withDrawResponse.setName(updatedAccount.getName());
        withDrawResponse.setEmail(updatedAccount.getEmail());
        withDrawResponse.setBalance(updatedAccount.getBalance());


        return withDrawResponse;

    }

    @Transactional
    public CustomerTransferResponse transferForCurrentCustomer(CustomerTransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
        validateAmount(request.getAmount());

        CustomerProfileResponse customer = customerClient.getMyProfile();
        String firstAccountNumber = request.getFromAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? request.getFromAccountNumber() : request.getToAccountNumber();
        String secondAccountNumber = firstAccountNumber.equals(request.getFromAccountNumber())
                ? request.getToAccountNumber() : request.getFromAccountNumber();
        Account firstLocked = accountRepository.findByAccountNumberForUpdate(firstAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        Account secondLocked = accountRepository.findByAccountNumberForUpdate(secondAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        Account source = firstLocked.getAccountNumber().equals(request.getFromAccountNumber()) ? firstLocked : secondLocked;
        Account destination = firstLocked.getAccountNumber().equals(request.getToAccountNumber()) ? firstLocked : secondLocked;

        if (!source.getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("The source account does not belong to the signed-in customer");
        }
        validateAccountForTransaction(source);
        validateAccountForTransaction(destination);
        if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())
                || !source.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalArgumentException("Source, destination and transfer currencies must match");
        }
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        source.setUpdatedAt(LocalDateTime.now());
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        destination.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(source);
        accountRepository.save(destination);

        return new CustomerTransferResponse(
                customer.getCustomerId(),
                source.getAccountNumber(),
                destination.getAccountNumber(),
                source.getCurrency());
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

    public Page<AccountResponse> allBranchAccount(String branchCode,int page,int size) {
        Page<Account> createdAt = accountRepository.findByBranchCode(branchCode, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<AccountResponse> map = createdAt.map(account -> mapToResponse(account));
        return map;
    }
   @Transactional
    public String depositIntoAc(String accountNumber,BigDecimal amount) {
        try {
            Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("account does not exists"));
            account.setBalance(amount);
            accountRepository.save(account);

            return "deposit successfully";
        }catch (Exception e){
            return e.getMessage();
        }
    }

    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }

    private void publishLoanTransactionEvent(Account account, BigDecimal amount, String transactionType) {
        LoanTransactionEvent event = new LoanTransactionEvent();
        String eventId = UUID.randomUUID().toString();
        event.setEventId(eventId);
        event.setTransactionId(eventId.length() > 12
                ? "TXN-" + eventId.replace("-", "").substring(0, 12).toUpperCase()
                : eventId);
        event.setCustomerId(account.getCustomerId());
        event.setCustomerName(account.getName());
        event.setEmail(account.getEmail());
        event.setTransactionType(transactionType);
        event.setAmount(amount.abs());
        event.setCurrency(account.getCurrency());
        event.setTransactionStatus("SUCCESS");
        event.setReference("LOAN-" + requestReferenceSuffix(transactionType));
        event.setDescription("LOAN".equals(transactionType) ? "Loan disbursement" : "Loan repayment");
        event.setBranchCode(account.getBranchCode());
        if ("DISBURSEMENT".equals(transactionType)) {
            event.setFromAccountNumber("NOVA-LOAN-FUND");
            event.setToAccountNumber(account.getAccountNumber());
        } else {
            event.setFromAccountNumber(account.getAccountNumber());
            event.setToAccountNumber("NOVA-LOAN-ACCOUNT");
        }
        loanTransactionEventPublisher.publish(event);
    }

    private String requestReferenceSuffix(String transactionType) {
        return transactionType + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
