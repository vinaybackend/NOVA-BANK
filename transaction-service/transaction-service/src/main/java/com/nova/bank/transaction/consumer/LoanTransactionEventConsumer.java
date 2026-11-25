package com.nova.bank.transaction.consumer;

import com.nova.bank.transaction.entities.Transaction;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import com.nova.bank.transaction.repositories.TransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoanTransactionEventConsumer {
    private final TransactionRepository transactionRepository;

    public LoanTransactionEventConsumer(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "transaction-topic", groupId = "loan-transaction-history")
    public void consumeLoanEvent(LoanTransactionEvent event) {
        if (event == null || event.getTransactionId() == null
                || !("DISBURSEMENT".equalsIgnoreCase(event.getTransactionType())
                    || "PAYMENT".equalsIgnoreCase(event.getTransactionType()))) {
            return;
        }
        if (transactionRepository.existsByTransactionId(event.getTransactionId())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String accountNumber = "DISBURSEMENT".equalsIgnoreCase(event.getTransactionType())
                ? event.getToAccountNumber() : event.getFromAccountNumber();
        Transaction transaction = Transaction.builder()
                .transactionId(event.getTransactionId())
                .accountHolderName(event.getCustomerName())
                .accountNumber(accountNumber)
                .fromAccountNumber(event.getFromAccountNumber())
                .toAccountNumber(event.getToAccountNumber())
                .customerId(event.getCustomerId())
                .branchCode(event.getBranchCode())
                .transactionType(TransactionType.LOAN)
                .amount(event.getAmount())
                .currency(event.getCurrency() == null ? "INR" : event.getCurrency())
                .transactionStatus(TransactionStatus.SUCCESS)
                .reference(event.getReference())
                .description("DISBURSEMENT".equalsIgnoreCase(event.getTransactionType())
                        ? "Loan amount disbursed" : "EMI payment successful")
                .performedBy("SYSTEM")
                .createdAt(now)
                .updatedAt(now)
                .build();
        transactionRepository.save(transaction);
    }
}

