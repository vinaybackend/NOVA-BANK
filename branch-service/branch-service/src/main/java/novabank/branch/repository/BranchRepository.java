package novabank.branch.repository;

import novabank.branch.entities.Branch;
import novabank.branch.entities.BranchStatus;
import novabank.branch.entities.BranchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByBranchCodeIgnoreCase(String branchCode);

    boolean existsByIfscCodeIgnoreCase(String ifscCode);

    Optional<Branch> findByBranchCodeIgnoreCase(String branchCode);

    Optional<Branch> findByIfscCodeIgnoreCase(String ifscCode);

    Page<Branch> findByStatus(BranchStatus status, Pageable pageable);

    Page<Branch> findByCityContainingIgnoreCase(String city, Pageable pageable);

    Page<Branch> findByStateContainingIgnoreCase(String state, Pageable pageable);

    Page<Branch> findByBranchNameContainingIgnoreCase(String branchName, Pageable pageable);

    Page<Branch> findByBranchCodeContainingIgnoreCase(
            String branchCode, Pageable pageable);

    Page<Branch> findByIfscCodeContainingIgnoreCase(
            String ifscCode, Pageable pageable);

    Page<Branch> findByBranchType(BranchType branchType, Pageable pageable);

    Page<Branch> findByCityContainingIgnoreCaseAndStatus(String city, BranchStatus status, Pageable pageable);

    Optional<Branch> findByBranchCode(String branchCode);
}
