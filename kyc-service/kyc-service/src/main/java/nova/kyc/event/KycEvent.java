package nova.kyc.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KycEvent {
    String name;
    String verificationStatus;
    String kycId;
    String branchName;
    LocalDateTime createdAt;
    String email;
    String username;
    String password;


}
