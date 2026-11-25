package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {
    Optional<LoanAccount> findByApplicationId(String applicationId);
    List<LoanAccount> findByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from LoanAccount account where account.id = :id")
    java.util.Optional<LoanAccount> findByIdForUpdate(@Param("id") String id);
}
