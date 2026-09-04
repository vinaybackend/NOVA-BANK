package com.nova.bank.kyc.repositories;

import com.nova.bank.kyc.entities.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository extends JpaRepository<Kyc, Long> {

    Optional<Kyc> findByKycId(String kycId);

    Optional<Kyc> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    boolean existsByDocumentNumber(String documentNumber);
}
