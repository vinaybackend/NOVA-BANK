package com.nova.bank.repositories;
import com.nova.bank.entities.KycAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycAuditRepository extends JpaRepository<KycAudit, Long> {

    List<KycAudit> findByKycIdOrderByPerformedAtAsc(String kycId);
}