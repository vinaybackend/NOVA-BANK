package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.Account;
import com.nova.bank.account.entities.AccountType;
import com.nova.bank.account.entities.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountId(String accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.customerId = :customerId "
            + "and account.accountType = :accountType and account.accountStatus = :accountStatus "
            + "order by account.id")
    List<Account> findActiveAccountsForUpdate(@Param("customerId") String customerId,
                                               @Param("accountType") AccountType accountType,
                                               @Param("accountStatus") AccountStatus accountStatus);
    Optional<Account> findByCustomerIdAndAccountType(String customerId, AccountType accountType);
    List<Account> findByCustomerId(String customerId);

    List<Account> findByBranchCode(String branchCode);

    boolean existsByAccountId(String accountId);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByCustomerIdAndAccountType(String customerId, AccountType accountType);

    List<Account> findByCustomerIdAndBranchCode(String customerId, String branchCode);
    Page<Account> findByBranchCode(String branchCode, Pageable pageable);


}