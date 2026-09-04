package com.nova.bank.controllers;

import com.nova.bank.config.AppConstant;
import com.nova.bank.dto.*;
import com.nova.bank.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/create")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId
    ) {

        CustomerResponse response =
                customerService.getCustomerByCustomerId(customerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponse<CustomerResponse>> getAllTrain(@RequestParam(value = "page",defaultValue = AppConstant.page) int page,
                                                                      @RequestParam(value = "size", defaultValue = AppConstant.page_size) int size,
                                                                      @RequestParam(value = "sortBy",defaultValue = "id") String sortBy,
                                                                      @RequestParam(value = "sortDir" , defaultValue = "asc") String sortDir){

        PageResponse<CustomerResponse> allCustomers = customerService.getAllCustomers(page, size, sortBy, sortDir);

        return new ResponseEntity<>(allCustomers,HttpStatus.OK);

    }

    @PutMapping("/update/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerRequest request) {

        CustomerResponse response = customerService.updateCustomer(customerId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerStatusRequest request) {

        CustomerResponse response = customerService.updateCustomerStatus(customerId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{customerId}/close")
    public ResponseEntity<CustomerResponse> closeCustomer(@PathVariable String customerId) {

        CustomerResponse response = customerService.closeCustomer(customerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam String email) {

        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    @GetMapping("/search/mobile")
    public ResponseEntity<CustomerResponse> getCustomerByMobile(@RequestParam String mobileNumber) {

        return ResponseEntity.ok(customerService.getCustomerByMobile(mobileNumber));
    }
}

