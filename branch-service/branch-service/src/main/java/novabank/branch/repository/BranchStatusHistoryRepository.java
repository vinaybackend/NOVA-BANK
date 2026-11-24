package novabank.branch.repository;

import novabank.branch.entities.BranchStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchStatusHistoryRepository extends JpaRepository<BranchStatusHistory, Long> {

    List<BranchStatusHistory> findByBranchCodeOrderByChangedAtDesc(String branchCode);
}