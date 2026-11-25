package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {
    List<RepaymentSchedule> findByLoanAccountIdOrderByInstallmentNumber(String loanAccountId);

    List<RepaymentSchedule> findByDueDateBetweenAndStatusNot(LocalDate start, LocalDate end, String status);

    List<RepaymentSchedule> findByDueDateBeforeAndStatus(LocalDate dueDate, String status);
}