package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, String> {
    List<LoanPayment> findByLoanAccountIdOrderByPaidAtDesc(String loanAccountId);

    java.util.Optional<LoanPayment> findByIdAndLoanAccountId(String id, String loanAccountId);
}
