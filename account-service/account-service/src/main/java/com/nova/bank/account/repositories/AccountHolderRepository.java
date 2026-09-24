package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.AccountHolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountHolderRepository
        extends JpaRepository<AccountHolder, Long> {

    List<AccountHolder> findByAccountId(String accountId);

    Optional<AccountHolder> findByAccountIdAndCustomerId(String accountId, String customerId);

    boolean existsByAccountIdAndCustomerId(String accountId, String customerId);

    long countByAccountId(String accountId);
}