package com.nova.bank.account.repositories;

import com.nova.bank.account.entities.Branch;
import com.nova.bank.account.entities.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchId(String branchId);

    Optional<Branch> findByBranchCode(String branchCode);

    Optional<Branch> findByIfsc(String ifsc);

    boolean existsByBranchId(String branchId);

    boolean existsByBranchCode(String branchCode);

    boolean existsByIfsc(String ifsc);

    boolean existsByBranchIdAndStatus(String branchId, BranchStatus status);
}