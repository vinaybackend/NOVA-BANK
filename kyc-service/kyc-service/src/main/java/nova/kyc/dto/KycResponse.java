package nova.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponse {
    String kycId;
    String customerId;
    String kycType;
    String branchCode;
    String branchName;
    String branchIfsc;
    String verificationStatus;
    String reviewerId;
    String rejectionReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<KycDocumentResponse> documents;
}