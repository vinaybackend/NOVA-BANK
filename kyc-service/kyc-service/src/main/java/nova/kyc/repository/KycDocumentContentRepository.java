package nova.kyc.repository;

import nova.kyc.entity.KycDocumentContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycDocumentContentRepository extends JpaRepository<KycDocumentContentEntity, String> {
}
