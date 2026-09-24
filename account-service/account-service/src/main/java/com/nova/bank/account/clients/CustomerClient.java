package com.nova.bank.account.clients;

import org.springframework.cloud.openfeign.FeignClient;
import com.nova.bank.account.dto.CustomerEligibilityResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service",configuration = FeignClientConfig.class)
public interface CustomerClient {

    @GetMapping("/api/v1/customers/account-eligibility/{customerId}")
    CustomerEligibilityResponse getAccountEligibility(@PathVariable("customerId") String customerId);
}
