package com.nova.bank.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.bank.entities.KycDocument;
import com.nova.bank.projectConfig.AppConstant;
import com.nova.bank.dto.*;
import com.nova.bank.repositories.KycDocumentRepository;
import com.nova.bank.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final ObjectMapper objectMapper;
    private final CustomerService customerService;
    private final KycDocumentRepository kycDocumentRepository;

    public CustomerController(ObjectMapper objectMapper, CustomerService customerService, KycDocumentRepository kycDocumentRepository) {
        this.objectMapper = objectMapper;
        this.customerService = customerService;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','HR')")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerResponse> createCustomer(@RequestPart("request") String request, @RequestPart("files") List<MultipartFile> files) throws IOException {
        System.out.println("Files received: " + files.size());
        // Convert JSON String into CustomerOnboardingRequest object
        CustomerOnboardingRequest onboardingRequest = objectMapper.readValue(request, CustomerOnboardingRequest.class);

        System.out.println("Customer Name: " + onboardingRequest.getFirstName() + " " + onboardingRequest.getLastName());

        // Send both JSON data and uploaded files to service
        CustomerResponse response = customerService.createCustomer(onboardingRequest, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER')")
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String customerId) {

        CustomerResponse response = customerService.getCustomerByCustomerId(customerId);

        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER')")
    @GetMapping("/all")
    public ResponseEntity<PageResponse<CustomerResponse>> getALlCustomer(@RequestParam(value = "page", defaultValue = AppConstant.page) int page,
                                                                         @RequestParam(value = "size", defaultValue = AppConstant.page_size) int size,
                                                                         @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
                                                                         @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        PageResponse<CustomerResponse> allCustomers = customerService.getAllCustomers(page, size, sortBy, sortDir);

        return new ResponseEntity<>(allCustomers, HttpStatus.OK);

    }

    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER')")
    @PutMapping("/update/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerRequest request) {

        CustomerResponse response = customerService.updateCustomer(customerId, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER')")
    @PatchMapping("/status/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerStatusRequest request) {

        CustomerResponse response = customerService.updateCustomerStatus(customerId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER')")
    @PatchMapping("/{customerId}/close")
    public ResponseEntity<CustomerResponse> closeCustomer(@PathVariable String customerId) {

        CustomerResponse response = customerService.closeCustomer(customerId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER')")
    @GetMapping("/search/{email}")
    public ResponseEntity<Object> getCustomerByEmail(@PathVariable String email, Authentication authentication) {

        Object customerByEmail = customerService.getCustomerByEmail(email, authentication);
        return new ResponseEntity<>(customerByEmail,HttpStatus.OK);
    }

//    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER')")
//    @GetMapping("/search/{mobile}")
//    public ResponseEntity<CustomerResponse> getCustomerByMobile(@PathVariable String mobileNumber) {
//
//        return ResponseEntity.ok(customerService.getCustomerByMobile(mobileNumber));
//    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER')")
    @PutMapping("active/{customerId}")
    public void statusUpdates(@PathVariable String customerId) {
        customerService.statusUpdate(customerId);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @GetMapping("/kyc/documents/{documentId}")
    public ResponseEntity<Resource> getKycDocument(@PathVariable String documentId) throws IOException {

        KycDocument document = kycDocumentRepository.findByDocumentId(documentId).orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        Path filePath = Paths.get(document.getFileUrl());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + documentId);
        }

        Resource resource = new UrlResource(filePath.toUri());

        String contentType = Files.probeContentType(filePath);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(Files.size(filePath))
                .body(resource);
    }

    @PreAuthorize("hasRole('KYC_OFFICER')")
    @GetMapping("/account-eligibility/{customerId}")
    public CustomerEligibilityResponse getAccountEligibility(@PathVariable String customerId) {

        CustomerEligibilityResponse response = customerService.getAccountEligibility(customerId);

        return response;
    }
}