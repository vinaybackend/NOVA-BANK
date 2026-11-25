package nova.kyc.repository;

import nova.kyc.entity.KycApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycApplicationRepository extends JpaRepository<KycApplicationEntity, String> {
    List<KycApplicationEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<KycApplicationEntity> findAllByOrderByCreatedAtDesc();

    Page<KycApplicationEntity> findByBranchCode(String branchCode, Pageable pageable);
}
