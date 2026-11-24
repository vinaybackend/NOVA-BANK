package nova.kyc.client;
import lombok.Data;

@Data
public class AuthRegistrationRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
    private String roleName;
    private String branchName;
    private String branchCode;
    private String ifsc;
}
