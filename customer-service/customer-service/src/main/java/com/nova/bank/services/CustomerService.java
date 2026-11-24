package com.nova.bank.services;

import com.nova.bank.client.AccountClient;
import com.nova.bank.client.AccountResponse;
import com.nova.bank.client.BranchClient;
import com.nova.bank.client.BranchResponse;
import com.nova.bank.client.CreateAccountRequest;
import com.nova.bank.dto.*;
import com.nova.bank.dto.request.CustomerUpdatedRequest;
import com.nova.bank.entities.*;
import com.nova.bank.exception.*;
import com.nova.bank.repositories.CustomerRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountClient accountClient;
    private final BranchClient branchClient;
    private final CustomerImageStorageService imageStorageService;
    @Value("${nova.upload.customer-dir}")
    private String customerUploadDirectory;

    public CustomerService(CustomerRepository customerRepository, AccountClient accountClient, BranchClient branchClient, CustomerImageStorageService imageStorageService) {
        this.customerRepository = customerRepository;
        this.accountClient = accountClient;
        this.branchClient = branchClient;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerOnboardingRequest request, MultipartFile image) {

        validateDuplicateCustomer(request);

        // Validate branch existence and ACTIVE status
        if (request.getBranchCode() == null || request.getBranchCode().isBlank()) {
            throw new IllegalArgumentException("Branch code is required for onboarding");
        }

        BranchResponse branch;
        try {
            branch = branchClient.getBranchById(request.getBranchCode());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Branch not found with code: " + request.getBranchCode());
        }

        if (branch == null || !"ACTIVE".equalsIgnoreCase(branch.getStatus())) {
            throw new IllegalStateException("Cannot onboard customer. Branch is " + (branch == null ? "unknown" : branch.getStatus()));
        }

        String customerId = generateCustomerId();
        String imageFileName = imageStorageService.storeImage(image, customerId);

        Customer customer = Customer.builder()
                .customerId(customerId)
                .title(request.getTitle())
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                // Use authoritative branch info from Branch Service
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .ifscCode(branch.getIfscCode())
                .openingBalance(request.getOpeningBalance())
                .nationality(request.getNationality())
                .maritalStatus(request.getMaritalStatus())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .alternateMobileNumber(request.getAlternateMobileNumber())
                .permanentAddress(request.getPermanentAddress())
                .permanentCity(request.getPermanentCity())
                .permanentState(request.getPermanentState())
                .permanentCountry(request.getPermanentCountry())
                .permanentPinCode(request.getPermanentPinCode())
                .profileImageUrl(imageFileName)
                .communicationAddress(request.getCommunicationAddress())
                .communicationCity(request.getCommunicationCity())
                .communicationState(request.getCommunicationState())
                .communicationCountry(request.getCommunicationCountry())
                .communicationPinCode(request.getCommunicationPinCode())
                .occupation(request.getOccupation())
                .annualIncome(request.getAnnualIncome())
                .sourceOfIncome(request.getSourceOfIncome())
                .language(request.getLanguage())
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .status(CustomerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    private void validateDuplicateCustomer(CustomerOnboardingRequest request) {
        if (customerRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("Customer already exists with mobile number");
        }
        if (request.getEmail() != null && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Customer already exists with email");
        }
    }

    private String generateCustomerId() {
        return "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getTitle(),
                customer.getName(),
                customer.getBranchName(),
                customer.getBranchCode(),
                customer.getIfscCode(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNationality(),
                customer.getMaritalStatus(),
                customer.getEmail(),
                customer.getMobileNumber(),
                customer.getAlternateMobileNumber(),
                customer.getPermanentAddress(),
                customer.getPermanentCity(),
                customer.getPermanentState(),
                customer.getPermanentCountry(),
                customer.getPermanentPinCode(),
                customer.getCommunicationAddress(),
                customer.getCommunicationCity(),
                customer.getCommunicationState(),
                customer.getCommunicationCountry(),
                customer.getCommunicationPinCode(),
                customer.getOccupation(),
                customer.getAnnualIncome(),
                customer.getSourceOfIncome(),
                customer.getLanguage(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getOpeningBalance(),
                customer.getProfileImageUrl()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Customer> customerPage = customerRepository.findAll(pageRequest);
        Page<CustomerResponse> responsePage = customerPage.map(this::mapToResponse);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomersByBranch(
            String branchId, int page, int size, String sortBy, String sortDir) {
        validateCustomerPage(page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        List<String> customerIds = accountClient.getCustomerIdsByBranchId(branchId);
        Page<Customer> customers = customerIds.isEmpty()
                ? Page.empty(pageRequest)
                : customerRepository.findByCustomerIdIn(customerIds, pageRequest);
        return PageResponse.fromPage(customers.map(this::mapToResponse));
    }

    public CustomerResponse getCustomerByCustomerIdForBranch(String customerId, String branchId) {
        requireCustomerInBranch(customerId, branchId);
        return getCustomerByCustomerId(customerId);
    }

    @Transactional
    public CustomerResponse updateCustomerForBranch(String customerId, UpdateCustomerRequest request, String branchId) {
        requireCustomerInBranch(customerId, branchId);
        return updateCustomer(customerId, request);
    }

    @Transactional
    public CustomerResponse updateCustomerStatusForBranch(
            String customerId, UpdateCustomerStatusRequest request, String branchId) {
        requireCustomerInBranch(customerId, branchId);
        return updateCustomerStatus(customerId, request);
    }

    @Transactional
    public CustomerResponse closeCustomerForBranch(String customerId, String branchId) {
        requireCustomerInBranch(customerId, branchId);
        return closeCustomer(customerId);
    }

    private void requireCustomerInBranch(String customerId, String branchId) {
        if (branchId == null || branchId.isBlank()
                || !accountClient.getCustomerIdsByBranchId(branchId).contains(customerId)) {
            throw new AccessDeniedException("You can only manage customers with accounts at your assigned branch");
        }
    }

    private void validateCustomerPage(int page, int size, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("Page number cannot be negative");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");
        if (sortBy == null || sortBy.isBlank()) throw new IllegalArgumentException("Sort field is required");
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }
    }

    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        if (request.email() != null
                && !request.email().equalsIgnoreCase(customer.getEmail())
                && customerRepository.existsByEmail(request.email())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists");
        }
        if (request.mobileNumber() != null
                && !request.mobileNumber().equals(customer.getMobileNumber())
                && customerRepository.existsByMobileNumber(request.mobileNumber())) {
            throw new CustomerAlreadyExistsException("Customer with mobile number already exists");
        }

        if (request.title() != null) customer.setTitle(request.title());
        if (request.name() != null) customer.setName(request.name());
        if (request.nationality() != null) customer.setNationality(request.nationality());
        if (request.maritalStatus() != null) customer.setMaritalStatus(request.maritalStatus());
        if (request.email() != null) customer.setEmail(request.email());
        if (request.mobileNumber() != null) customer.setMobileNumber(request.mobileNumber());
        if (request.alternateMobileNumber() != null) customer.setAlternateMobileNumber(request.alternateMobileNumber());
        if (request.permanentAddress() != null) customer.setPermanentAddress(request.permanentAddress());
        if (request.permanentCity() != null) customer.setPermanentCity(request.permanentCity());
        if (request.permanentState() != null) customer.setPermanentState(request.permanentState());
        if (request.permanentCountry() != null) customer.setPermanentCountry(request.permanentCountry());
        if (request.permanentPinCode() != null) customer.setPermanentPinCode(request.permanentPinCode());
        if (request.communicationAddress() != null) customer.setCommunicationAddress(request.communicationAddress());
        if (request.communicationCity() != null) customer.setCommunicationCity(request.communicationCity());
        if (request.communicationState() != null) customer.setCommunicationState(request.communicationState());
        if (request.communicationCountry() != null) customer.setCommunicationCountry(request.communicationCountry());
        if (request.communicationPinCode() != null) customer.setCommunicationPinCode(request.communicationPinCode());
        if (request.occupation() != null) customer.setOccupation(request.occupation());
        if (request.annualIncome() != null) customer.setAnnualIncome(request.annualIncome());
        if (request.sourceOfIncome() != null) customer.setSourceOfIncome(request.sourceOfIncome());
        if (request.language() != null) customer.setLanguage(request.language());
        if (request.accountType() != null) customer.setAccountType(request.accountType());
        if (request.dateOfBirth() != null) customer.setDateOfBirth(request.dateOfBirth());
        if (request.currency() != null) customer.setCurrency(request.currency());

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToResponse(updatedCustomer);
    }

    private void validateStatusTransition(CustomerStatus currentStatus, CustomerStatus newStatus) {
        switch (currentStatus) {
            case PENDING -> throw new IllegalStateException(
                    "Pending customers can only become active after KYC approval");
            case ACTIVE -> {
                if (newStatus != CustomerStatus.INACTIVE
                        && newStatus != CustomerStatus.BLOCKED
                        && newStatus != CustomerStatus.CLOSED) {
                    throw new IllegalStateException("ACTIVE customer can only become INACTIVE, BLOCKED or CLOSED");
                }
            }
            case INACTIVE -> {
                if (newStatus != CustomerStatus.ACTIVE && newStatus != CustomerStatus.CLOSED) {
                    throw new IllegalStateException("INACTIVE customer can only become ACTIVE or CLOSED");
                }
            }
            case BLOCKED -> {
                if (newStatus != CustomerStatus.ACTIVE && newStatus != CustomerStatus.CLOSED) {
                    throw new IllegalStateException("BLOCKED customer can only become ACTIVE or CLOSED");
                }
            }
            case CLOSED -> throw new IllegalStateException("CLOSED customer cannot change status");
        }
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(String customerId, UpdateCustomerStatusRequest request) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        CustomerStatus currentStatus = customer.getStatus();
        CustomerStatus newStatus = request.getStatus();
        validateStatusTransition(currentStatus, newStatus);
        customer.setStatus(newStatus);
        Customer updatedCustomer = customerRepository.save(customer);
        return mapToResponse(updatedCustomer);
    }

    public CustomerResponse getCustomerByCustomerId(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customer not found: " + customerId));
        return mapToResponse(customer);
    }

    public CustomerResponse closeCustomer(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customer not found:" + customerId));
        customer.setStatus(CustomerStatus.CLOSED);
        Customer save = customerRepository.save(customer);
        return mapToResponse(save);
    }

    public Object getCustomerByEmail(String email, Authentication authentication) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + email));

        if (!customer.getEmail().equals(authentication.getName())) {
            throw new EmailMissMatch("Login email and provided email do not match: " + email);
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            return "Kyc is pending";
        }
        List<AccountResponse> accounts = accountClient.getAccountsByCustomerId(customer.getCustomerId());
        return CustomerAccountResponse.builder()
                .account(accounts.isEmpty() ? null : accounts.get(0))
                .accounts(accounts)
                .build();
    }

    public @Nullable CustomerResponse getCustomerByMobile(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found:" + mobileNumber));
        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerEligibilityResponse getAccountEligibility(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        boolean eligible = customer.getStatus() == CustomerStatus.ACTIVE;
        return CustomerEligibilityResponse.builder()
                .customerId(customer.getCustomerId())
                .customerStatus(customer.getStatus())
                .eligible(eligible)
                .build();
    }

    @Transactional
    public void activateCustomerAfterKycApproval(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        if (customer.getStatus() == CustomerStatus.PENDING) {
            customer.setStatus(CustomerStatus.ACTIVE);
            customerRepository.save(customer);
        } else if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Cannot open an account for a " + customer.getStatus() + " customer");
        }
    }

    public AccountResponse openAccountAfterKycApproval(String customerId, String preferredBranchCode) {

        String branchCode = preferredBranchCode;

        if (branchCode.isBlank()) {
            throw new IllegalStateException(" branch is not configured");
        }

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        if (customer.getStatus() != CustomerStatus.PENDING
                && customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Cannot open an account for a " + customer.getStatus() + " customer");
        }

        CreateAccountRequest request = CreateAccountRequest.builder()
                .customerId(customer.getCustomerId())
                .accountType(customer.getAccountType())
                .balance(customer.getOpeningBalance())
                .currency(customer.getCurrency())
                .branchCode(branchCode)
                .name(customer.getName())
                .email(customer.getEmail())
                .mobileNumber(customer.getMobileNumber())
                .build();

        return accountClient.openAccountAfterKycApproval(request);
    }

    public CustomerResponse getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        return mapToResponse(customer);
    }

    public Page<CustomerResponse> allCustomerBranch(String branchCode, int page, int size) {
        Page<Customer> createdAt = customerRepository.findByBranchCode(branchCode, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<CustomerResponse> responsePage = createdAt.map(customer -> mapToResponse(customer));
        return responsePage;
    }

    public CustomerResponse updateCustomerProfile(CustomerUpdatedRequest request, Authentication authentication) {
        String email = authentication.getName();
        System.out.println(email);
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        if (request.getMobileNumber() != null) {
            customer.setMobileNumber(request.getMobileNumber());
        }
        if (request.getAlternateMobileNumber() != null) {
            customer.setAlternateMobileNumber(request.getAlternateMobileNumber());
        }
        if (request.getPermanentAddress() != null) {
            customer.setPermanentAddress(request.getPermanentAddress());
        }
        if (request.getPermanentCity() != null) {
            customer.setPermanentCity(request.getPermanentCity());
        }
        if (request.getPermanentState() != null) {
            customer.setPermanentState(request.getPermanentState());
        }
        if (request.getPermanentCountry() != null) {
            customer.setPermanentCountry(request.getPermanentCountry());
        }
        if (request.getPermanentPinCode() != null) {
            customer.setPermanentPinCode(request.getPermanentPinCode());
        }
        if (request.getCommunicationAddress() != null) {
            customer.setCommunicationAddress(request.getCommunicationAddress());
        }
        if (request.getCommunicationCity() != null) {
            customer.setCommunicationCity(request.getCommunicationCity());
        }
        if (request.getCommunicationState() != null) {
            customer.setCommunicationState(request.getCommunicationState());
        }
        if (request.getCommunicationCountry() != null) {
            customer.setCommunicationCountry(request.getCommunicationCountry());
        }
        if (request.getCommunicationPinCode() != null) {
            customer.setCommunicationPinCode(request.getCommunicationPinCode());
        }
        if (request.getOccupation() != null) {
            customer.setOccupation(request.getOccupation());

        }
        Customer save = customerRepository.save(customer);
        return mapToResponse(save);
    }


    public ResponseEntity<Resource> getCustomerImage(String customerId) throws MalformedURLException {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));

        String imageName = customer.getProfileImageUrl();

        if (imageName == null || imageName.isBlank()) {
            throw new RuntimeException("Customer image not found");
        }

        Path imagePath = Paths.get(customerUploadDirectory).resolve(imageName).normalize();

        if (!imagePath.startsWith(Paths.get(customerUploadDirectory).normalize())
                || !Files.exists(imagePath)) {
            throw new RuntimeException("Customer image not found");
        }

        Resource resource = new UrlResource(imagePath.toUri());

        try {
            String contentType = Files.probeContentType(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            contentType != null ? contentType : "application/octet-stream"))
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Unable to access customer image", e);
        }
    }
}