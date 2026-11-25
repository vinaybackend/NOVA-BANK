package com.nova.bank.loan.client;

import com.nova.bank.loan.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CUSTOMER-SERVICE", configuration = FeignAuthConfig.class)
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{customerId}")
    CustomerProfileResponse getCustomer(@PathVariable String customerId);

    @GetMapping("/api/v1/customers/me")
    CustomerResponse getMyProfile();
}
