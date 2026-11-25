package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanApplication;
import com.nova.bank.loan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {
    List<LoanApplication> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<LoanApplication> findByAssignedOfficerIdOrderByCreatedAtDesc(String assignedOfficerId);

    @Query("""
            select application from LoanApplication application
            where application.assignedOfficerId = :officerId
               or (application.assignedOfficerId is null and application.status in :availableStatuses)
            order by application.createdAt desc
            """)
    List<LoanApplication> findOfficerInbox(@Param("officerId") String officerId,
                                           @Param("availableStatuses") List<LoanStatus> availableStatuses);

    long countByStatus(LoanStatus status);

    default List<LoanApplication> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from LoanApplication application where application.id = :id")
    java.util.Optional<LoanApplication> findByIdForUpdate(@Param("id") String id);
}
