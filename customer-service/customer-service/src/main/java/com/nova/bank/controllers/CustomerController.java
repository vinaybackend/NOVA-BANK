package com.nova.bank.controllers;
import com.nova.bank.dto.request.CustomerUpdatedRequest;
import com.nova.bank.projectConfig.AppConstant;
import com.nova.bank.dto.*;
import com.nova.bank.client.AccountResponse;
import com.nova.bank.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerResponse> createCustomer(@RequestPart("customer") String customerJson, @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        CustomerOnboardingRequest request = mapper.readValue(customerJson, CustomerOnboardingRequest.class);

        CustomerResponse response = customerService.createCustomer(request, profileImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','LOAN_OFFICER','BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String customerId) {
        CustomerResponse response = customerService.getCustomerByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<PageResponse<CustomerResponse>> getALlCustomer(
            @RequestParam(value = "page", defaultValue = AppConstant.page) int page,
            @RequestParam(value = "size", defaultValue = AppConstant.page_size) int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
            Authentication authentication) {

        PageResponse<CustomerResponse> allCustomers = customerService.getAllCustomers(page, size, sortBy, sortDir);
        return new ResponseEntity<>(allCustomers, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PutMapping("/update/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/status/{customerId}")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(@PathVariable String customerId, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        CustomerResponse response = customerService.updateCustomerStatus(customerId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/{customerId}/close")
    public ResponseEntity<CustomerResponse> closeCustomer(@PathVariable String customerId) {
        CustomerResponse response = customerService.closeCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @GetMapping("/search/{email}")
    public ResponseEntity<Object> getCustomerByEmail(@PathVariable String email, Authentication authentication) {
        Object customerByEmail = customerService.getCustomerByEmail(email, authentication);
        return new ResponseEntity<>(customerByEmail, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER', 'ADMIN')")
    @GetMapping("/account-eligibility/{customerId}")
    public CustomerEligibilityResponse getAccountEligibility(@PathVariable String customerId) {
        return customerService.getAccountEligibility(customerId);
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER', 'ADMIN','BRANCH_MANAGER')")
    @PostMapping("/{customerId}/kyc-approval/open-account")
    public ResponseEntity<AccountResponse> openAccountAfterKycApproval(
            @PathVariable String customerId,
            @RequestParam(required = false) String preferredBranchCode) {
        AccountResponse account = customerService.openAccountAfterKycApproval(customerId, preferredBranchCode);
        return ResponseEntity.ok(account);
    }

    // NEW: Activate customer after KYC approval
    @PreAuthorize("hasAnyRole('KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/{customerId}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable String customerId) {
        customerService.activateCustomerAfterKycApproval(customerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        CustomerResponse response = customerService.getMyProfile(authentication);
        return ResponseEntity.ok(response);
    }

    private boolean isBranchManagerOnly(Authentication authentication) {
        boolean isBranchManager = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_BRANCH_MANAGER"::equals);
        boolean isGlobalStaff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_KYC_OFFICER"));
        return isBranchManager && !isGlobalStaff;
    }

    private String getBranchId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String branchCode = jwt.getClaimAsString("branchCode");
            if (branchCode != null && !branchCode.isBlank()) return branchCode;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "A branch assignment is required for branch manager access");
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER','HR')")
    @GetMapping("/all-customer/{branchCode}")
    public ResponseEntity<Page<CustomerResponse>> allCustomerByBranchCode(@PathVariable("branchCode") String branchCode,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size){
        Page<CustomerResponse> customerResponses = customerService.allCustomerBranch(branchCode, page, size);
        return new ResponseEntity<>(customerResponses,HttpStatus.OK);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/update-customer-profile")
    public ResponseEntity<CustomerResponse> updateCustomerProfile(@Valid @RequestBody CustomerUpdatedRequest request, Authentication authentication) {
        CustomerResponse customerUpdate = customerService.updateCustomerProfile(request, authentication);
        return ResponseEntity.ok(customerUpdate);
    }

    @GetMapping("/{customerId}/image")
    public ResponseEntity<Resource> getCustomerImage(@PathVariable String customerId) throws MalformedURLException {

        return customerService.getCustomerImage(customerId);
    }
}