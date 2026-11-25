package nova.kyc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_document_content")
@Getter
@Setter
@NoArgsConstructor
public class KycDocumentContentEntity {

    @Id
    @Column(name = "document_id", length = 30, nullable = false)
    private String documentId;

    @Lob
    @Column(name = "content", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] content;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "preferred_branch_ifsc", length = 11)
    private String branchIfsc;

}
