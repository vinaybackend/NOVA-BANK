package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.Account;
import com.nova.bank.account.entities.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountId(String accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    boolean existsByAccountId(String accountId);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerIdAndAccountType(String customerId, AccountType accountType);
}