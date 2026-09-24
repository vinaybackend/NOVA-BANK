package com.nova.bank.repositories;
import com.nova.bank.entities.Kyc;
import com.nova.bank.entities.KycVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository extends JpaRepository<Kyc, Long> {

    Optional<Kyc> findByKycId(String kycId);

    Optional<Kyc> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    <T> Page<T> findByKycVerificationStatus(KycVerificationStatus kycVerificationStatus, PageRequest pageRequest);

}
