
package com.nova.bank.transaction.repositories;

import com.nova.bank.transaction.entities.Transaction;
import com.nova.bank.transaction.entities.TransactionStatus;
import com.nova.bank.transaction.entities.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByReference(String reference);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findByCustomerId(String customerId);

    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    List<Transaction> findByAccountNumberAndTransactionType(String accountNumber, TransactionType transactionType);

    List<Transaction> findByAccountNumberAndTransactionStatus(String accountNumber, TransactionStatus transactionStatus);

    List<Transaction> findByTransactionTypeAndTransactionStatusAndBranchCodeOrderByCreatedAtAsc(
            TransactionType transactionType,
            TransactionStatus transactionStatus,
            String branchCode);

    List<Transaction> findByTransactionTypeAndBranchCodeOrderByCreatedAtDesc(
            TransactionType transactionType,
            String branchCode,
            Pageable pageable);

    Page<Transaction> findByBranchCode(String branchCode,Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Transaction t
            SET t.transactionStatus = :processing,
                t.updatedAt = :now
            WHERE t.transactionId = :transactionId
              AND t.transactionType = :type
              AND t.branchCode = :branchCode
              AND t.transactionStatus = :pending
            """)
    int claimPendingDeposit(
            @Param("transactionId") String transactionId,
            @Param("branchCode") String branchCode,
            @Param("type") TransactionType type,
            @Param("pending") TransactionStatus pending,
            @Param("processing") TransactionStatus processing,
            @Param("now") LocalDateTime now);

    boolean existsByTransactionId(String transactionId);

    boolean existsByReference(String reference);
}
