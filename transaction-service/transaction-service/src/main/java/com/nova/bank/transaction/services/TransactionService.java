package com.nova.bank.transaction.services;

import com.nova.bank.transaction.clients.AccountClient;
import com.nova.bank.transaction.clients.CustomerClient;
import com.nova.bank.transaction.clients.response.WithDrawResponse;
import com.nova.bank.transaction.dto.*;
import com.nova.bank.transaction.entities.Transaction;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import com.nova.bank.transaction.exceptions.InsufficientBalanceException;
import com.nova.bank.transaction.producer.TransactionEvent;
import com.nova.bank.transaction.producer.TransactionEventPublisher;
import com.nova.bank.transaction.repositories.TransactionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final TransactionEventPublisher transactionEventPublisher;


    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient, CustomerClient customerClient, TransactionEventPublisher transactionEventPublisher) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.customerClient = customerClient;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyRecentTransactions() {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getCustomerId(), PageRequest.of(0, 50)).stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return transactionRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    public TransactionResponse deposit(CreateDepositRequest request, String performedBy, String performedByRole, String branchCode) {
        AccountResponse account = accountClient.getAccountByNumber(request.getAccountNumber());
        validateDepositAccount(account, request);
        validateOperatorBranch(account, branchCode);
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .branchCode(account.getBranchCode())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .transactionStatus(TransactionStatus.PENDING)
                .reference(generateReference())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();
        transactionRepository.saveAndFlush(transaction);
        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentDeposits(String branchId) {
        return transactionRepository.findByTransactionTypeAndBranchCodeOrderByCreatedAtDesc(
                TransactionType.DEPOSIT, requireBranchId(branchId), PageRequest.of(0, 100))
                .stream().map(this::mapToResponse).toList();
    }

    private void validateDepositAccount(AccountResponse account, CreateDepositRequest request) {
        if (account.getAccountStatus() == null || !"ACTIVE".equals(account.getAccountStatus().name())) {
            throw new IllegalArgumentException("Deposits are only available for active accounts");
        }
        if (!account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalArgumentException("Transaction currency does not match account currency");
        }
    }

    private void validateOperatorBranch(AccountResponse account, String branchCode) {
        if (!requireBranchId(branchCode).equals(account.getBranchCode())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only record deposits for accounts at your assigned branch");
        }
    }

    private String requireBranchId(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A branch assignment is required for deposit operations");
        }
        return branchId;
    }

    private String generateTransactionId() {

        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0,6).toUpperCase();
    }

    private String generateReference() {

        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0,6).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountNumber(transaction.getAccountNumber())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountNumber(transaction.getToAccountNumber())
                .customerId(transaction.getCustomerId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(transaction.getTransactionStatus())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .performedBy(transaction.getPerformedBy())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    @Transactional
    public TransactionResponse withdraw(CreateWithdrawalRequest request) {

        // 1. Get account
        AccountResponse account = accountClient.getAccountByNumber(request.getAccountNumber());

        // 2. Validate currency
        if (!account.getCurrency().equalsIgnoreCase(request.getCurrency())) {

            throw new IllegalArgumentException("Transaction currency does not match account currency");
        }

        // 3. Generate transaction ID
        String transactionId = generateTransactionId();
        // 4. Generate reference
        String reference = generateReference();

        LocalDateTime now = LocalDateTime.now();

        // 5. Debit account
        WithDrawResponse withDrawResponse = accountClient.debitAccount(request.getAccountNumber(), request.getAmount());

        // 6. Create successful transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .accountNumber(withDrawResponse.getAccountNumber())
                .customerId(withDrawResponse.getCustomerId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .accountHolderName(withDrawResponse.getName())
                .currency(withDrawResponse.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(reference)
                .performedBy(SecurityContextHolder.getContext().getAuthentication().getName())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    @Transactional
    public TransferTransactionResponse transfer(CreateTransferRequest request) {

        // Source and destination cannot be same
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {

            throw new InsufficientBalanceException("Source and destination accounts cannot be the same");
        }

        // Get source account
        AccountResponse sourceAccount = accountClient.getAccountByNumber(request.getFromAccountNumber());

        // Get destination account
        AccountResponse destinationAccount = accountClient.getAccountByNumber(request.getToAccountNumber());

        // Check currency
        if (!sourceAccount.getCurrency().equalsIgnoreCase(destinationAccount.getCurrency())) {

            throw new IllegalArgumentException("Source and destination account currencies do not match");
        }

        // Check requested currency
        if (!sourceAccount.getCurrency().equalsIgnoreCase(request.getCurrency())) {

            throw new IllegalArgumentException("Transaction currency does not match account currency");
        }

        // Debit source account
        accountClient.debitAccount(request.getFromAccountNumber(), request.getAmount());

        // Credit destination account
        accountClient.creditAccount(request.getToAccountNumber(), request.getAmount());

        // Create transaction
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .fromAccountNumber(sourceAccount.getAccountNumber())
                .toAccountNumber(destinationAccount.getAccountNumber())
                .customerId(sourceAccount.getCustomerId())
                .transactionType(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .currency(sourceAccount.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(generateReference())
                .performedBy(SecurityContextHolder.getContext().getAuthentication().getName())
                .description(request.getDescription())
                .createdAt(now).updatedAt(now)
                .build();

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        //  Return response
        return mapToResponseTransfer(savedTransaction);
    }

    public TransferTransactionResponse transferForCurrentCustomer(CreateTransferRequest request) {
        CustomerProfileResponse customer = customerClient.getMyProfile();
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .customerId(customer.getCustomerId())
                .transactionType(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(generateReference())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .performedBy(SecurityContextHolder.getContext().getAuthentication().getName())
                .build();
        //for credit

        // find destinatio account
        AccountResponse accountByNumber = accountClient.getAccountByNumber(request.getToAccountNumber());

        transactionRepository.saveAndFlush(transaction);

        //into which account(receiver)
        Transaction credit = Transaction.builder()
                .transactionId(generateTransactionId())
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .customerId(accountByNumber.getCustomerId())
                .transactionType(TransactionType.CREDIT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(generateReference())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();
        transactionRepository.saveAndFlush(credit);

        CustomerTransferAccountResponse transfer;
        try {
            transfer = accountClient.transferForCurrentCustomer(request);
        }
        catch (RuntimeException transferError) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            try {
                transactionRepository.saveAndFlush(transaction);
            } catch (RuntimeException persistenceError) {
                transferError.addSuppressed(persistenceError);
            }
            throw transferError;
        }

        transaction.setFromAccountNumber(transfer.getFromAccountNumber());
        transaction.setToAccountNumber(transfer.getToAccountNumber());
        transaction.setCustomerId(transfer.getCustomerId());
        transaction.setCurrency(transfer.getCurrency());
        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setUpdatedAt(LocalDateTime.now());
        Transaction success= transactionRepository.saveAndFlush(transaction);


        //even produce
        TransactionEvent transactionEvent=new TransactionEvent();
        transactionEvent.setEventId(UUID.randomUUID().toString().substring(0,8));
        transactionEvent.setCustomerId(success.getCustomerId());
        transactionEvent.setAmount(success.getAmount());
        transactionEvent.setCurrency(success.getCurrency());
        transactionEvent.setDescription(success.getDescription());
        transactionEvent.setCustomerName(customer.getName());
        transactionEvent.setTransactionId(success.getTransactionId());
        transactionEvent.setTransactionStatus(success.getTransactionStatus().toString());
        transactionEvent.setTransactionType(success.getTransactionType().toString());
        transactionEvent.setPerformedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        transactionEvent.setReference(success.getReference());
        transactionEvent.setToAccountNumber(success.getToAccountNumber());
        transactionEvent.setFromAccountNumber(success.getFromAccountNumber());
        transactionEvent.setEmail(customer.getEmail());

        transactionEventPublisher.publishTransactionCreatedEvent(transactionEvent);
        // wrote for mails


        return mapToResponseTransfer(success);
    }

    private TransferTransactionResponse mapToResponseTransfer(Transaction transaction) {

        return TransferTransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .customerId(transaction.getCustomerId())
                .transactionType(transaction.getTransactionType())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountNumber(transaction.getToAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(transaction.getTransactionStatus())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    public Page<TransactionResponse> findAlLTransBranch(String branchCode, int page, int size) {
        Page<Transaction> transactions = transactionRepository.findByBranchCode(branchCode, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return transactions.map(transaction -> mapToResponse(transaction));
    }
}
