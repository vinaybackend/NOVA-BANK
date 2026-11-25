package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.LoanBalanceOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanBalanceOperationRepository extends JpaRepository<LoanBalanceOperation, String> {
}
