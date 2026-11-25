package nova.kyc.dto;

import lombok.Data;

@Data
public class KycAccountResponse {
    private String accountId;
    private String accountNumber;
    private String accountStatus;
    private String branchName;
    private String currency;
}
