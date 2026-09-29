package com.nova.bank.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service",configuration = FeignClientConfig.class)
public interface AccountClient {
    @PostMapping("/api/v1/accounts/create")
    public AccountResponse openAccount(@RequestBody CreateAccountRequest createAccountRequest);

    @GetMapping("/api/v1/accounts/customer/{customerId}")
    public AccountResponse getAccountByCustomerId(@PathVariable String customerId);

}
