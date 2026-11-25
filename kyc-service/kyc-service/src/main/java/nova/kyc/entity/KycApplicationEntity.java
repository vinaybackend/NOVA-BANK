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
@Table(name = "kyc_applications")
@Getter
@Setter
@NoArgsConstructor
public class KycApplicationEntity {

    @Id
    @Column(name = "kyc_id", length = 30, nullable = false)
    private String kycId;

    @Column(name = "customer_id", length = 30, nullable = false)
    private String customerId;

    @Column(name = "kyc_type", length = 50, nullable = false)
    private String kycType;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "preferred_branch_ifsc", length = 11)
    private String branchIfsc;

    @Column(name = "verification_status", length = 20, nullable = false)
    private String verificationStatus;

    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}