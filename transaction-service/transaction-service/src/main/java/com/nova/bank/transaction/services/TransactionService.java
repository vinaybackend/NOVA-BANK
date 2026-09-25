package com.nova.bank.transaction.services;

import com.nova.bank.transaction.clients.AccountClient;
import com.nova.bank.transaction.dto.*;
import com.nova.bank.transaction.entities.Transaction;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import com.nova.bank.transaction.exceptions.InsufficientBalanceException;
import com.nova.bank.transaction.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    @Transactional
    public TransactionResponse deposit(CreateDepositRequest request) {
        // Get account
        AccountResponse account = accountClient.getAccountByNumber(request.getAccountNumber());
        // Validate currency
        if (!account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalArgumentException("Transaction currency does not match account currency");
        }
        // Create transaction ID
        String transactionId = generateTransactionId();
        //Create reference
        String reference = generateReference();
        LocalDateTime now = LocalDateTime.now();
        // Credit account
        accountClient.creditAccount(request.getAccountNumber(), request.getAmount());
        // Create successful transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(reference)
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    private String generateTransactionId() {

        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateReference() {

        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountNumber(transaction.getAccountNumber())
                .customerId(transaction.getCustomerId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionStatus(transaction.getTransactionStatus())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
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
        accountClient.debitAccount(request.getAccountNumber(), request.getAmount());

        // 6. Create successful transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(reference)
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
                .description(request.getDescription())
                .createdAt(now).updatedAt(now)
                .build();

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        //  Return response
        return mapToResponseTransfer(savedTransaction);
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
}
