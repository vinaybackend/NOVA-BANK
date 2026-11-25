package nova.kyc.client;

import nova.kyc.dto.KycAccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "customer-service", configuration = KycFeignClientConfig.class)
public interface CustomerClient {

    @PostMapping("/api/v1/customers/{customerId}/kyc-approval/open-account")
    KycAccountResponse openAccountAfterKycApproval(
            @PathVariable String customerId,
            @RequestParam("preferredBranchCode") String preferredBranchCode // Must match CustomerController
    );

    @PostMapping("/api/v1/customers/{customerId}/activate")
    void activateCustomer(@PathVariable String customerId);


}