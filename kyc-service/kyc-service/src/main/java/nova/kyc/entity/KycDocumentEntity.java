package nova.kyc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
public class KycDocumentEntity {

    @Id
    @Column(name = "document_id", length = 30, nullable = false)
    private String documentId;

    @Column(name = "kyc_id", length = 30, nullable = false)
    private String kycId;

    @Column(name = "document_type", length = 30, nullable = false)
    private String documentType;

    @Column(name = "document_number", length = 30)
    private String documentNumber;

    @Column(name = "file_name", length = 100)
    private String fileName;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "branch_ifsc", length = 11)
    private String branchIfsc;


    @Column(name = "verification_status", length = 20, nullable = false)
    private String verificationStatus;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
