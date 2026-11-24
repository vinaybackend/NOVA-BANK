package novabank.employee.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface AuthClient {
    @PostMapping("/api/v1/admin/auth/employee/register")
    Long registerEmployeeAuth(@RequestBody AuthRegistrationRequest request);
}
