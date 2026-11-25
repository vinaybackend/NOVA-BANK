package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationHistoryRepository extends JpaRepository<LoanApplicationHistory, String> {
    List<LoanApplicationHistory> findByApplicationIdOrderByCreatedAtAsc(String applicationId);
}
