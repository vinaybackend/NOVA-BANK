package com.nova.bank.kyc.services;

import com.nova.bank.kyc.clients.CustomerClient;
import com.nova.bank.kyc.dto.*;
import com.nova.bank.kyc.entities.Kyc;
import com.nova.bank.kyc.entities.KycVerificationStatus;
import com.nova.bank.kyc.exceptions.*;
import com.nova.bank.kyc.repositories.KycRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import feign.FeignException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class KycService {

    private final KycRepository kycRepository;
    private final CustomerClient customerClient;

    public KycService(KycRepository kycRepository, CustomerClient customerClient) {
        this.kycRepository = kycRepository;
        this.customerClient = customerClient;
    }
    @Transactional
    public KycResponse createKyc(CreateKycRequest request) {

        try {

            customerClient.searchByCustomerId(request.getCustomerId());

        } catch (FeignException.NotFound exception) {

            throw new CustomerNotFoundException("Customer not found: " + request.getCustomerId());

        } catch (FeignException exception) {

            throw new CustomerServiceUnavailableException("Customer Service is currently unavailable");
        }

        if (kycRepository.existsByCustomerId(request.getCustomerId())) {
            throw new KycAlreadyExistsException("KYC already exists for customer: " + request.getCustomerId());
        }

        if (kycRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateDocumentException("Document is already registered");
        }

        Kyc kyc = Kyc.builder()
                .kycId(generateKycId())
                .customerId(request.getCustomerId())
                .kycType(request.getKycType())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .verificationStatus(KycVerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Kyc savedKyc = kycRepository.save(kyc);

        return mapToResponse(savedKyc);
    }


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
                .kycDocumentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .kycVerificationStatus(kyc.getVerificationStatus())
                .reviewerId(kyc.getReviewerId())
                .verifiedAt(kyc.getVerifiedAt())
                .rejectionReason(kyc.getRejectionReason())
                .createdAt(kyc.getCreatedAt())
                .updatedAt(kyc.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public KycResponse getKycById(String kycId) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

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

        if (kyc.getVerificationStatus() != KycVerificationStatus.PENDING) {

            throw new InvalidKycStatusException("KYC can be moved to UNDER_REVIEW only from PENDING status");
        }
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;

        String reviewerId = jwtAuthenticationToken.getToken().getSubject();

        String reviewerName = jwtAuthenticationToken.getToken().getClaimAsString("name");
        kyc.setReviewerId(reviewerId);
        kyc.setReviewerName(reviewerName);
        kyc.setVerificationStatus(KycVerificationStatus.UNDER_REVIEW);

        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }


    // reject

    @Transactional
    public KycResponse rejectKyc(String kycId, RejectKycRequest request) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getVerificationStatus() != KycVerificationStatus.UNDER_REVIEW) {
            throw new InvalidKycStatusException("KYC can be moved to UNDER_REVIEW only from PENDING status");
        }

        kyc.setVerificationStatus(KycVerificationStatus.REJECTED);
        kyc.setRejectionReason(request.getRejectionReason());
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }

    // approve

    @Transactional
    public KycResponse approveKyc(String kycId) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getVerificationStatus() != KycVerificationStatus.UNDER_REVIEW) {
            throw new InvalidKycStatusException("KYC can be approved only from UNDER_REVIEW status");
        }

        kyc.setVerificationStatus(KycVerificationStatus.APPROVED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);
        if(updatedKyc.getVerificationStatus().equals(KycVerificationStatus.APPROVED)){
            customerClient.customerStatusUpdated(updatedKyc.getCustomerId());
        }

        return mapToResponse(updatedKyc);
    }

    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatusByCustomerId(String customerId) {

        Kyc kyc = kycRepository.findByCustomerId(customerId).orElseThrow(() -> new KycNotFoundException("KYC not found for customer: " + customerId));

        return new KycStatusResponse(kyc.getCustomerId(), kyc.getVerificationStatus().name());
    }
//
//    @Transactional
//    public KycResponse submitKyc(String kycId) {
//
//        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new RuntimeException("KYC not found: " + kycId));
//
//        // KYC can be submitted only from DRAFT
//        if (kyc.getVerificationStatus() != KycVerificationStatus.PENDING) {
//
//            throw new RuntimeException("KYC cannot be submitted from status: " + kyc.getVerificationStatus());
//        }
//
//        kyc.setVerificationStatus(KycVerificationStatus.SUBMITTED);
//
//        Kyc submittedKyc = kycRepository.save(kyc);
//
//        return mapToResponse(submittedKyc);
//    }
}
