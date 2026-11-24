package nova.kyc.client;

import nova.kyc.dto.BranchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "branch-service", configuration = KycFeignClientConfig.class)
public interface BranchClient {

    @GetMapping("/api/v1/branches/{branchCode}")
    BranchResponse getBranchById(@PathVariable("branchCode") String branchCode);
}