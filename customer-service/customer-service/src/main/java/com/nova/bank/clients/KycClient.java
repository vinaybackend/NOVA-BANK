package com.nova.bank.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kyc-service")
public interface KycClient {

    @GetMapping("/api/v1/kycs/customer/{customerId}")
    KycStatusResponse getKycStatus(@PathVariable("customerId") String customerId);
}
