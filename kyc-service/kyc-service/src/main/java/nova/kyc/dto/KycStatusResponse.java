package nova.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusResponse {
    private String kycId;
    private String verificationStatus;
    private String reviewerId;
    private String rejectionReason;
    private LocalDateTime updatedAt;
    private KycAccountResponse account;
}
