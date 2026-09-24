package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.AccountNominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountNomineeRepository extends JpaRepository<AccountNominee, Long> {

    Optional<AccountNominee> findByNomineeId(String nomineeId);

    List<AccountNominee> findByAccountId(String accountId);

    Optional<AccountNominee> findByAccountIdAndActiveTrue(String accountId);

    boolean existsByNomineeId(String nomineeId);

    boolean existsByAccountId(String accountId);
}