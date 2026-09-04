package com.nova.bank.kyc.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name ="customer-service")
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{customerId}")
    public CustomerResponse searchByCustomerId(@PathVariable String customerId);


}
