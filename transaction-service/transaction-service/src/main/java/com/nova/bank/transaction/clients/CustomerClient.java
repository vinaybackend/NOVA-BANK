package com.nova.bank.transaction.clients;

import com.nova.bank.transaction.dto.CustomerProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "customer-service", configuration = FeignClientConfig.class)
public interface CustomerClient {
    @GetMapping("/api/v1/customers/me")
    CustomerProfileResponse getMyProfile();
}
