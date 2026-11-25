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
public class KycDocumentResponse {
    private String documentId;
    private String documentType;
    private String documentNumber;
    private String fileName;
    private String verificationStatus;
    private LocalDateTime uploadedAt;
}
