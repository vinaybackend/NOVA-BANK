package com.nova.bank.loan.client;

import com.nova.bank.loan.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "KYC-SERVICE", configuration = FeignAuthConfig.class)
public interface KycClient {

    @GetMapping("/api/v1/kyc/customer/{customerId}")
    List<KycResponse> getCustomerKyc(@PathVariable String customerId);
}
