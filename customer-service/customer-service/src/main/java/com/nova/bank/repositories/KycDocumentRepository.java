package com.nova.bank.repositories;
import com.nova.bank.entities.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByKycId(String kycId);

    boolean existsByDocumentNumber(String documentNumber);

    Optional<KycDocument> findByDocumentId(String documentId);

}
