package nova.kyc.repository;

import nova.kyc.entity.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocumentEntity, String> {
    List<KycDocumentEntity> findAllByKycIdInOrderByUploadedAtDesc(Collection<String> kycIds);

    List<KycDocumentEntity> findAllByKycIdOrderByUploadedAtDesc(String kycId);

    Optional<KycDocumentEntity> findByKycIdAndDocumentId(String kycId, String documentId);
}
