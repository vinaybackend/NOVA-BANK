package com.nova.bank.account.clients;

import com.nova.bank.account.dto.KycResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kyc-service")
public interface KycClient {

    @GetMapping("/api/v1/kycs/customer/{customerId}")
    KycResponse getKycByCustomerId(@PathVariable("customerId") String customerId);
}