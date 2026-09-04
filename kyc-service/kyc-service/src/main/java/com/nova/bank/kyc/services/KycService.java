package com.nova.bank.kyc.services;

import com.nova.bank.kyc.clients.CustomerClient;
import com.nova.bank.kyc.clients.CustomerResponse;
import com.nova.bank.kyc.dto.CreateKycRequest;
import com.nova.bank.kyc.dto.KycResponse;
import com.nova.bank.kyc.dto.RejectKycRequest;
import com.nova.bank.kyc.dto.ReviewKycRequest;
import com.nova.bank.kyc.entities.Kyc;
import com.nova.bank.kyc.entities.VerificationStatus;
import com.nova.bank.kyc.exceptions.*;
import com.nova.bank.kyc.repositories.KycRepository;
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
                .verificationStatus(VerificationStatus.PENDING)
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
                .documentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .verificationStatus(kyc.getVerificationStatus())
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
    public KycResponse reviewKyc(String kycId, ReviewKycRequest request) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new InvalidKycStatusException("KYC can be moved to UNDER_REVIEW only from PENDING status");
        }
        kyc.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        kyc.setReviewerId(request.getReviewerId());
        kyc.setUpdatedAt(LocalDateTime.now());
        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }


    // reject

    @Transactional
    public KycResponse rejectKyc(String kycId, RejectKycRequest request) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getVerificationStatus() != VerificationStatus.UNDER_REVIEW) {
            throw new InvalidKycStatusException("KYC can be moved to UNDER_REVIEW only from PENDING status");
        }

        kyc.setVerificationStatus(VerificationStatus.REJECTED);
        kyc.setRejectionReason(request.getRejectionReason());
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }

    // approve

    @Transactional
    public KycResponse approveKyc(String kycId) {

        Kyc kyc = kycRepository.findByKycId(kycId).orElseThrow(() -> new KycNotFoundException("KYC not found: " + kycId));

        if (kyc.getVerificationStatus() != VerificationStatus.UNDER_REVIEW) {
            throw new InvalidKycStatusException("KYC can be approved only from UNDER_REVIEW status");
        }

        kyc.setVerificationStatus(VerificationStatus.APPROVED);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setUpdatedAt(LocalDateTime.now());

        Kyc updatedKyc = kycRepository.save(kyc);

        return mapToResponse(updatedKyc);
    }
}
