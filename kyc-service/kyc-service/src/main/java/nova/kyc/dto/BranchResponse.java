package nova.kyc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchResponse {
    private Long id;
    private String branchCode;
    private String branchName;
    private String ifscCode;
    private String status;
    private String city;
    private String state;
}
