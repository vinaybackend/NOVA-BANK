package com.nova.bank.services;
import com.nova.bank.config.AppConstant;
import com.nova.bank.dto.*;
import com.nova.bank.entities.*;
import com.nova.bank.exception.*;
import com.nova.bank.repositories.CustomerRepository;
import com.nova.bank.repositories.KycAuditRepository;
import com.nova.bank.repositories.KycDocumentRepository;
import com.nova.bank.repositories.KycRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KycService {

    private final KycRepository kycRepository;
    private final KycAuditRepository kycAuditRepository;
    private final CustomerRepository customerRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final ModelMapper modelMapper;

    public KycService(KycRepository kycRepository, KycAuditRepository kycAuditRepository, CustomerRepository customerRepository, KycDocumentRepository kycDocumentRepository, ModelMapper modelMapper) {
        this.kycRepository = kycRepository;
        this.kycAuditRepository = kycAuditRepository;
        this.customerRepository = customerRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.modelMapper = modelMapper;
    }
//
//    @Transactional
//    public KycResponse createKyc(CreateKycRequest request) {
//
//        try {
//
//          kycRepository.findByCustomerId(request.getCustomerId()).orElseThrow(()->new CustomerNotFoundException("customer not found ; " +request.getCustomerId()));
//        } catch (CustomerNotFoundException exception) {
//
//            throw new CustomerNotFoundException("Customer not found: " + request.getCustomerId());
//        }
//
//        if (kycRepository.existsByCustomerId(request.getCustomerId())) {
//            throw new KycAlreadyExistsException("KYC already exists for customer: " + request.getCustomerId());
//        }
//
//        if (kycRepository.existsByDocumentNumber(request.getDocumentNumber())) {
//            throw new DuplicateDocumentException("Document is already registered");
//        }
//
//        Kyc kyc = Kyc.builder()
//                .kycId(generateKycId())
//                .customerId(request.getCustomerId())
//                .kycType(request.getKycType())
//                .documentType(request.getDocumentType())
//                .documentNumber(request.getDocumentNumber())
//                .kycVerificationStatus(kycVerificationStatus.PENDING)
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//        Kyc save = kycRepository.save(kyc);
//
//        return mapToResponse(save);
//    }


    private String generateKycId() {
        return "KYC-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private KycResponse mapToResponse(Kyc kyc) {

        return KycResponse.builder()
                .kycId(kyc.getKycId())
                .customerId(kyc.getCustomerId())
                .kycType(kyc.getKycType())
                .kycVerificationStatus(kyc.getKycVerificationStatus())
                .reviewerId(kyc.getReviewerId())
                .verifiedAt(kyc.getVerifiedAt())
                .rejectionReason(kyc.getRejectionReason())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .reviewerName(kyc.getReviewerName())
                .build();
    }

    @Transactional(readOnly = true)
    public KycResponse getKycById(String kycId) {

        Kyc kyc = (Kyc) kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("kyc not found :" + kycId));


        return mapToResponse(kyc);
    }

    @Transactional(readOnly = true)
    public KycResponse getKycByCustomerId(String customerId) {

        Kyc kyc = kycRepository.findByCustomerId(customerId).orElseThrow(() -> new KycNotFoundException("KYC not found for customer: " + customerId));

        return mapToResponse(kyc);
    }

    @Transactional
    public KycResponse reviewKyc(String kycId, Authentication authentication) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getKycVerificationStatus() != KycVerificationStatus.PENDING) {

            throw new InvalidKycStatusException("KYC can be reviewed only from PENDING status");
        }

        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;

        String reviewerId = jwt.getToken().getSubject();

        String username = jwt.getToken().getClaimAsString("preferred_username");
        kyc.setKycVerificationStatus(KycVerificationStatus.UNDER_REVIEW);

        kyc.setReviewerId(reviewerId);
        kyc.setReviewerName(username);
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);
        List<KycDocument> byKycId = kycDocumentRepository.findByKycId(updatedKyc.getKycId());
        List<KycDocument> list1 = byKycId.stream().map(kycDocument -> {
            kycDocument.setVerificationStatus(KycDocumentVerificationStatus.UNDER_REVIEW);
            return kycDocumentRepository.save(kycDocument);
        }).toList();
        List<KycDocumentResponse> list = list1.stream().map(kycDocument -> mapToResponseDocument(kycDocument)).toList();

        createAudit(updatedKyc, KycAuditAction.UNDER_REVIEW, authentication, "KYC moved to UNDER_REVIEW");

        return mapToResponseKyc(updatedKyc,list);
    }

    private KycDocumentResponse mapToResponseDocument(KycDocument document) {

        return KycDocumentResponse.builder()
                .documentId(document.getDocumentId())
                .kycId(document.getKycId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .fileName(document.getFileName())
                .fileUrl(AppConstant.BASE_URL + "/api/v1/customers/kyc/documents/" + document.getDocumentId())                .kycVerificationStatus(document.getVerificationStatus())
                .uploadedAt(document.getUploadedAt())
                .verifiedAt(document.getVerifiedAt())
                .kycVerificationStatus(document.getVerificationStatus())
                .build();
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


    // reject


    @Transactional
    public KycResponse rejectKyc(String kycId, RejectKycRequest request, Authentication authentication) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getKycVerificationStatus() != KycVerificationStatus.UNDER_REVIEW) {

            throw new InvalidKycStatusException("KYC can be rejected only from UNDER_REVIEW status");
        }

        kyc.setKycVerificationStatus(KycVerificationStatus.REJECTED);

        kyc.setRejectionReason(request.getRejectionReason());

        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        createAudit(updatedKyc, KycAuditAction.REJECT, authentication, request.getRejectionReason());

        KycResponse kycResponse = mapToResponse(updatedKyc);
        List<KycDocument> byKycId = kycDocumentRepository.findByKycId(updatedKyc.getKycId());
        List<KycDocument> list1 = byKycId.stream().map(kycDocument -> {
            kycDocument.setVerificationStatus(KycDocumentVerificationStatus.REJECT);
            return kycDocumentRepository.save(kycDocument);
        }).toList();
        List<KycDocumentResponse> kycDocumentResponses = list1.stream().map(kycDocument -> mapToResponseDocument(kycDocument)).toList();
        kycResponse.setKycDocument(kycDocumentResponses);
        return kycResponse;

    }

    // approve

    @Transactional
    public CustomerResponse approveKyc(String kycId, Authentication authentication) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getKycVerificationStatus() != KycVerificationStatus.UNDER_REVIEW) {

            throw new InvalidKycStatusException("KYC can be approved only from UNDER_REVIEW status");
        }

        kyc.setKycVerificationStatus(KycVerificationStatus.APPROVED);

        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);
        Customer customer = customerRepository.findByCustomerId(updatedKyc.getCustomerId()).orElseThrow(() -> new CustomerNotFoundException("customer not found :" + kyc.getCustomerId()));
        customer.setStatus(CustomerStatus.ACTIVE);

        createAudit(updatedKyc, KycAuditAction.APPROVE, authentication, "KYC approved");
        KycResponse kycResponse = mapToResponse(updatedKyc);

        List<KycDocument> byKycId = kycDocumentRepository.findByKycId(updatedKyc.getKycId());

        List<KycDocument> list1 = byKycId.stream().map(kycDocument -> {
            kycDocument.setVerificationStatus(KycDocumentVerificationStatus.APPROVE);
            kycDocument.setVerifiedAt(LocalDateTime.now());
            return kycDocumentRepository.save(kycDocument);
        }).toList();

        List<KycDocumentResponse> list = list1.stream().map(kycDocument -> mapToResponseDocument(kycDocument)).toList();
        kycResponse.setKycDocument(list);
        Customer updatedCustomer = customerRepository.save(customer);
       return mapToCustomerResponse(updatedCustomer,kycResponse);
    }
    private CustomerResponse mapToCustomerResponse(Customer customer, KycResponse kycResponse) {

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
    public KycStatusResponse getKycStatusByCustomerId(String customerId) {

        Kyc kyc = kycRepository.findByCustomerId(customerId).orElseThrow(() -> new KycNotFoundException("KYC not found for customer: " + customerId));

        return new KycStatusResponse(kyc.getCustomerId(), kyc.getKycVerificationStatus().name());
    }
//
//    @Transactional
//    public KycResponse submitKyc(String kycId) {
//
//        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new RuntimeException("KYC not found: " + kycId));
//
//        // KYC can be submitted only from DRAFT
//        if (kyc.getKycVerificationStatus() != kycVerificationStatus.PENDING) {
//
//            throw new RuntimeException("KYC cannot be submitted from status: " + kyc.getKycVerificationStatus());
//        }
//
//        kyc.setKycVerificationStatus(kycVerificationStatus.SUBMITTED);
//
//        Kyc submittedKyc = kycRepository.save(kyc);
//
//        return mapToResponse(submittedKyc);
//    }

    private void createAudit(Kyc kyc, KycAuditAction action, Authentication authentication, String remarks) {

        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;

        String performedBy = jwtAuthenticationToken.getToken().getSubject();

        String performedByName = jwtAuthenticationToken.getToken().getClaimAsString("preferred_username");

        KycAudit audit = KycAudit.builder()
                .kycId(kyc.getKycId())
                .action(action)
                .performedBy(performedBy)
                .performedByName(performedByName)
                .performedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();
        kycAuditRepository.save(audit);
    }

    @Transactional
    public KycResponse resubmitKyc(String kycId) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getKycVerificationStatus() != KycVerificationStatus.REJECTED) {

            throw new InvalidKycStatusException("Only REJECTED KYC can be resubmitted");
        }

        kyc.setKycVerificationStatus(KycVerificationStatus.UNDER_REVIEW);

        kyc.setRejectionReason(null);
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }

    public PageResponse<KycResponse> allPendingKyc(int page, int size,String sortBy,String sortDir) {
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
        Page<Kyc> allPendingKyc = kycRepository.findByKycVerificationStatus(KycVerificationStatus.PENDING, pageRequest);
        Page<KycResponse> kycResponses = allPendingKyc.map(kyc -> modelMapper.map(kyc, KycResponse.class));

        return PageResponse.fromPage(kycResponses);

    }
}
