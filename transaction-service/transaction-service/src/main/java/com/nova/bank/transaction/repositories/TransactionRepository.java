package com.nova.bank.transaction.repositories;

import com.nova.bank.transaction.entities.Transaction;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByReference(String reference);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByCustomerId(String customerId);

    List<Transaction> findByAccountNumberAndTransactionType(String accountNumber, TransactionType transactionType);

    List<Transaction> findByAccountNumberAndTransactionStatus(String accountNumber, TransactionStatus transactionStatus);

    boolean existsByTransactionId(String transactionId);

    boolean existsByReference(String reference);
}