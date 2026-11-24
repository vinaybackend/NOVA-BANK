package com.nova.bank.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "account-service",configuration = FeignClientConfig.class)
public interface AccountClient {
    @PostMapping("/api/v1/accounts/create")
    public AccountResponse openAccount(@RequestBody CreateAccountRequest createAccountRequest);

    @PostMapping("/api/v1/accounts/open-after-kyc-approval")
    AccountResponse openAccountAfterKycApproval(@RequestBody CreateAccountRequest createAccountRequest);

    @GetMapping("/api/v1/accounts/customer/{customerId}")
    List<AccountResponse> getAccountsByCustomerId(@PathVariable String customerId);

    @GetMapping("/api/v1/accounts/branch/{branchId}/customer-ids")
    List<String> getCustomerIdsByBranchId(@PathVariable String branchId);

}
