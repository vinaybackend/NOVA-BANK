package nova.kyc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", configuration = KycFeignClientConfig.class)
public interface AuthClient {
    @PostMapping("/api/v1/auth/register")
    String registerEmployeeAuth(@RequestBody AuthRegistrationRequest request);
}
