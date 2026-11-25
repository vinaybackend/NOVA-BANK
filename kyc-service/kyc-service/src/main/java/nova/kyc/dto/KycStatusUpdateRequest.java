package nova.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusUpdateRequest {
    private String verificationStatus;
    private String reviewerId;
    private String rejectionReason;
}
