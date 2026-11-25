package com.nova.bank.loan.client;

import com.nova.bank.loan.config.FeignAuthConfig;
import com.nova.bank.loan.dto.CustomerBankAccountResponse;
import com.nova.bank.loan.dto.LoanBalanceRequest;
import com.nova.bank.loan.dto.LoanBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ACCOUNT-SERVICE", configuration = FeignAuthConfig.class)
public interface AccountClient {

    @GetMapping("/api/v1/accounts/customer/{customerId}")
    List<CustomerBankAccountResponse> getCustomerAccounts(@PathVariable("customerId") String customerId);

    @PostMapping("/api/v1/accounts/internal/loan-disbursement")
    LoanBalanceResponse disburseLoan(@RequestBody LoanBalanceRequest request);

    @PostMapping("/api/v1/accounts/internal/loan-payment")
    LoanBalanceResponse collectLoanPayment(@RequestBody LoanBalanceRequest request);
}
