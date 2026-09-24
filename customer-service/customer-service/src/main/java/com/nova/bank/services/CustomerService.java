package com.nova.bank.services;

import com.nova.bank.config.AppConstant;
import com.nova.bank.dto.*;
import com.nova.bank.entities.*;
import com.nova.bank.exception.*;
import com.nova.bank.repositories.CustomerRepository;
import com.nova.bank.repositories.KycDocumentRepository;
import com.nova.bank.repositories.KycRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KycRepository kycRepository;
    private final KycDocumentRepository kycDocumentRepository;

    private final Path folderPath = Paths.get("uploads", "kyc");

    @Transactional
    public CustomerResponse createCustomer(CustomerOnboardingRequest request, List<MultipartFile> files) throws IOException {

        // 1. Validate duplicate customer
        validateDuplicateCustomer(request);

        // 2. Validate KYC
        if (request.getKyc() == null) {
            throw new IllegalArgumentException("KYC information is required");
        }

        if (request.getKyc().getDocuments() == null || request.getKyc().getDocuments().isEmpty()) {

            throw new IllegalArgumentException("At least one KYC document is required");
        }

        // 3. Validate uploaded files
        if (files == null || files.size() != request.getKyc().getDocuments().size()) {

            throw new IllegalArgumentException("Number of uploaded files must match number of KYC documents");
        }

        // 4. Check duplicate document numbers
        for (KycDocumentRequest documentRequest : request.getKyc().getDocuments()) {

            if (kycDocumentRepository.existsByDocumentNumber(documentRequest.getDocumentNumber())) {

                throw new DuplicateDocumentException("Document is already registered: " + documentRequest.getDocumentType());
            }
        }

        // 5. Generate customer ID
        String customerId = generateCustomerId();

        // 6. Create Customer
        Customer customer = Customer.builder().customerId(customerId).title(request.getTitle()).firstName(request.getFirstName()).middleName(request.getMiddleName()).lastName(request.getLastName()).dateOfBirth(request.getDateOfBirth()).gender(request.getGender()).nationality(request.getNationality()).maritalStatus(request.getMaritalStatus()).email(request.getEmail()).mobileNumber(request.getMobileNumber()).alternateMobileNumber(request.getAlternateMobileNumber()).permanentAddress(request.getPermanentAddress()).permanentCity(request.getPermanentCity()).permanentState(request.getPermanentState()).permanentCountry(request.getPermanentCountry()).permanentPinCode(request.getPermanentPinCode()).communicationAddress(request.getCommunicationAddress()).communicationCity(request.getCommunicationCity()).communicationState(request.getCommunicationState()).communicationCountry(request.getCommunicationCountry()).communicationPinCode(request.getCommunicationPinCode()).occupation(request.getOccupation()).annualIncome(request.getAnnualIncome()).sourceOfIncome(request.getSourceOfIncome()).language(request.getLanguage()).build();

        Customer savedCustomer = customerRepository.save(customer);


        // 7. Check whether KYC already exists
        if (kycRepository.existsByCustomerId(savedCustomer.getCustomerId())) {

            throw new KycAlreadyExistsException("KYC already exists for customer: " + savedCustomer.getCustomerId());
        }


        // 8. Create KYC
        Kyc kyc = Kyc.builder().
                kycId(generateKycId())
                .customerId(savedCustomer.getCustomerId())
                .kycType(request.getKyc().getKycType())
                .kycVerificationStatus(KycVerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())

                .build();

        Kyc savedKyc = kycRepository.save(kyc);



        // 9. Create upload folder
        Path folder = Paths.get(folderPath.toUri());

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }


        // 10. Save each KYC document
        for (int i = 0; i < request.getKyc().getDocuments().size(); i++) {

            KycDocumentRequest documentRequest = request.getKyc().getDocuments().get(i);

            MultipartFile uploadedFile = files.get(i);


            // Validate file
            if (uploadedFile == null || uploadedFile.isEmpty()) {

                throw new IllegalArgumentException("File is required for " + documentRequest.getDocumentType());
            }


            // Get original filename
            String originalFileName = uploadedFile.getOriginalFilename();


            // Extract extension
            String extension = "";

            if (originalFileName != null && originalFileName.contains(".")) {

                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }


            // Generate secure storage filename
            String storedFileName = UUID.randomUUID() + extension;


            Path filePath = folder.resolve(storedFileName);


            // Save physical file
            Files.copy(uploadedFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


            // Create KYC document
            String contentType = uploadedFile.getContentType();

            KycDocument document = KycDocument.builder()
                    .documentId(generateDocumentId())
                    .kycId(savedKyc.getKycId())
                    .documentType(documentRequest.getDocumentType())
                    .documentNumber(documentRequest.getDocumentNumber())
                    .fileName(originalFileName)
                    .fileUrl(filePath.toString())
                    .contentType(contentType)
                    .verificationStatus(KycDocumentVerificationStatus.PENDING)
                    .build();
            KycDocument save = kycDocumentRepository.save(document);
        }
        List<KycDocument> kycDocuments = kycDocumentRepository.findByKycId(savedKyc.getKycId());
        List<KycDocumentResponse> listKycDocument = kycDocuments.stream().map(kycDocument -> mapToResponseDocument(kycDocument)).toList();

        KycResponse kycResponse = mapToResponseKyc(savedKyc,listKycDocument);


        // 12. Return response
        return mapToResponse(savedCustomer, kycResponse);
    }
    private String generateDocumentId() {
        return "DOC-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }
    private KycResponse mapToResponseKyc(Kyc kyc,List<KycDocumentResponse> kycDocuments) {

        return KycResponse.builder()
                .kycId(kyc.getKycId())
                .customerId(kyc.getCustomerId())
                .kycType(kyc.getKycType())
                .kycVerificationStatus(kyc.getKycVerificationStatus())
                .reviewerId(kyc.getReviewerId())
                .reviewerName(kyc.getReviewerName())
                .rejectionReason(kyc.getRejectionReason())
                .verifiedAt(kyc.getVerifiedAt())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .kycDocument(kycDocuments)
                .build();
    }
    private KycDocumentResponse mapToResponseDocument(KycDocument document) {

        return KycDocumentResponse.builder()
                .documentId(document.getDocumentId())
                .kycId(document.getKycId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .fileName(document.getFileName())
                .fileUrl(AppConstant.BASE_URL + "/api/v1/customers/kyc/documents/" + document.getDocumentId())
                .kycVerificationStatus(document.getVerificationStatus())
                .uploadedAt(document.getUploadedAt())
                .verifiedAt(document.getVerifiedAt())
                .build();
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

    private String generateKycId() {

        return "KYC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CustomerResponse mapToResponse(Customer customer, KycResponse kycResponse) {

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getTitle(),
                customer.getFirstName(),
                customer.getMiddleName(),
                customer.getLastName(),
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
                customer.getCommunicationCity(), customer.getCommunicationState(),
                customer.getCommunicationCountry(),
                customer.getCommunicationPinCode(), customer.getOccupation(),
                customer.getAnnualIncome(), customer.getSourceOfIncome(),
                customer.getLanguage(), customer.getStatus(), customer.getCreatedAt(),
                customer.getUpdatedAt(),
                kycResponse

        );

    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }


        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {

            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Customer> customerPage = customerRepository.findAll(pageRequest);

        Page<CustomerResponse> responsePage = customerPage.map(customer -> mapToResponse(customer, null));

        return PageResponse.fromPage(responsePage);
    }

    // update customer
    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {

        // Find existing customer
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        // Check duplicate email
        if (request.email() != null && !request.email().equalsIgnoreCase(customer.getEmail()) && customerRepository.existsByEmail(request.email())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists");
        }
        // Check duplicate mobile number
        if (request.mobileNumber() != null && !request.mobileNumber().equals(customer.getMobileNumber()) && customerRepository.existsByMobileNumber(request.mobileNumber())) {

            throw new CustomerAlreadyExistsException("Customer with mobile number already exists");
        }
        // Update only fields supplied by the client
        if (request.title() != null) {
            customer.setTitle(request.title());
        }

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }

        if (request.middleName() != null) {
            customer.setMiddleName(request.middleName());
        }

        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }

        if (request.nationality() != null) {
            customer.setNationality(request.nationality());
        }

        if (request.maritalStatus() != null) {
            customer.setMaritalStatus(request.maritalStatus());
        }

        if (request.email() != null) {
            customer.setEmail(request.email());
        }

        if (request.mobileNumber() != null) {
            customer.setMobileNumber(request.mobileNumber());
        }

        if (request.alternateMobileNumber() != null) {
            customer.setAlternateMobileNumber(request.alternateMobileNumber());
        }

        if (request.permanentAddress() != null) {
            customer.setPermanentAddress(request.permanentAddress());
        }

        if (request.permanentCity() != null) {
            customer.setPermanentCity(request.permanentCity());
        }

        if (request.permanentState() != null) {
            customer.setPermanentState(request.permanentState());
        }

        if (request.permanentCountry() != null) {
            customer.setPermanentCountry(request.permanentCountry());
        }

        if (request.permanentPinCode() != null) {
            customer.setPermanentPinCode(request.permanentPinCode());
        }

        if (request.communicationAddress() != null) {
            customer.setCommunicationAddress(request.communicationAddress());
        }

        if (request.communicationCity() != null) {
            customer.setCommunicationCity(request.communicationCity());
        }

        if (request.communicationState() != null) {
            customer.setCommunicationState(request.communicationState());
        }

        if (request.communicationCountry() != null) {
            customer.setCommunicationCountry(request.communicationCountry());
        }

        if (request.communicationPinCode() != null) {
            customer.setCommunicationPinCode(request.communicationPinCode());
        }


        if (request.occupation() != null) {
            customer.setOccupation(request.occupation());
        }

        if (request.annualIncome() != null) {
            customer.setAnnualIncome(request.annualIncome());
        }

        if (request.sourceOfIncome() != null) {
            customer.setSourceOfIncome(request.sourceOfIncome());
        }

        if (request.language() != null) {
            customer.setLanguage(request.language());
        }

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer, null);
    }

    private void validateStatusTransition(CustomerStatus currentStatus, CustomerStatus newStatus) {

        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != CustomerStatus.ACTIVE) {
                    throw new IllegalStateException("PENDING customer can only become ACTIVE");
                }
            }

            case ACTIVE -> {
                if (newStatus != CustomerStatus.INACTIVE && newStatus != CustomerStatus.BLOCKED && newStatus != CustomerStatus.CLOSED) {

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

            case CLOSED -> {
                throw new IllegalStateException("CLOSED customer cannot change status");
            }
        }
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(String customerId, UpdateCustomerStatusRequest request) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus currentStatus = customer.getStatus();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == CustomerStatus.ACTIVE) {

            try {
                Kyc kyc = kycRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(() -> new CustomerNotFoundException("kyc not found :" + customer.getCustomerId()));


                if (!"APPROVED".equalsIgnoreCase(kyc.getKycVerificationStatus().toString())) {

                    throw new InvalidCustomerStatusTransitionException("Customer cannot become ACTIVE because KYC is not APPROVED");
                }

            } catch (KycNotFoundException ex) {

                throw new KycNotFoundException("KYC record not found for customer: " + customerId);
            }
        }

        validateStatusTransition(currentStatus, newStatus);

        customer.setStatus(newStatus);

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer, null);
    }

    public CustomerResponse getCustomerByCustomerId(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("customer not found: " + customerId));
        return mapToResponse(customer, null);
    }

    public CustomerResponse closeCustomer(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("customer not found:" + customerId));
        customer.setStatus(CustomerStatus.CLOSED);
        Customer save = customerRepository.save(customer);
        return mapToResponse(save, null);
    }

    public @Nullable CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found:" + email));
        return mapToResponse(customer, null);
    }

    public @Nullable CustomerResponse getCustomerByMobile(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new CustomerNotFoundException("Customer not found:" + mobileNumber));
        return mapToResponse(customer, null);
    }

    public void statusUpdate(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found :" + customerId));
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerEligibilityResponse getAccountEligibility(String customerId) {

        // 1. Find customer
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        // 2. Find KYC
        Kyc kyc = kycRepository.findByCustomerId(customerId).orElseThrow(() -> new KycNotFoundException("KYC record not found for customer: " + customerId));

        // 3. Check customer status
        boolean customerActive = customer.getStatus() == CustomerStatus.ACTIVE;

        // 4. Check KYC status
        boolean kycApproved = kyc.getKycVerificationStatus() == KycVerificationStatus.APPROVED;

        // 5. Both conditions must be true
        boolean eligible = customerActive && kycApproved;

        // 6. Return only required information
        return CustomerEligibilityResponse.builder()
                .customerId(customer.getCustomerId())
                .customerStatus(customer.getStatus())
                .kycStatus(kyc.getKycVerificationStatus())
                .eligible(eligible)
                .build();
    }
}