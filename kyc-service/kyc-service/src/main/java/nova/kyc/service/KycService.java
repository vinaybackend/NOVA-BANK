package nova.kyc.service;

import nova.kyc.client.AuthClient;
import nova.kyc.client.AuthRegistrationRequest;
import nova.kyc.client.BranchClient;
import nova.kyc.client.CustomerClient;
import nova.kyc.dto.BranchResponse;
import nova.kyc.dto.KycAccountResponse;
import nova.kyc.dto.KycDocumentFile;
import nova.kyc.dto.KycDocumentRequest;
import nova.kyc.dto.KycDocumentResponse;
import nova.kyc.dto.KycResponse;
import nova.kyc.dto.KycStatusResponse;
import nova.kyc.dto.KycStatusUpdateRequest;
import nova.kyc.dto.KycSubmitRequest;
import nova.kyc.entity.KycApplicationEntity;
import nova.kyc.entity.KycDocumentContentEntity;
import nova.kyc.entity.KycDocumentEntity;
import nova.kyc.event.KycEvent;
import nova.kyc.event.KycEventPublisher;
import nova.kyc.repository.KycApplicationRepository;
import nova.kyc.repository.KycDocumentContentRepository;
import nova.kyc.repository.KycDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KycService {

    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private final KycApplicationRepository applicationRepository;
    private final KycDocumentRepository documentRepository;
    private final KycDocumentContentRepository documentContentRepository;
    private final CustomerClient customerClient;
    private final BranchClient branchClient;
    private final AuthClient authClient;
    private final KycEventPublisher kycEventPublisher;

    public KycService(KycApplicationRepository applicationRepository, KycDocumentRepository documentRepository, KycDocumentContentRepository documentContentRepository, CustomerClient customerClient, BranchClient branchClient, AuthClient authClient, KycEventPublisher kycEventPublisher) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.documentContentRepository = documentContentRepository;
        this.customerClient = customerClient;
        this.branchClient = branchClient;
        this.authClient = authClient;
        this.kycEventPublisher = kycEventPublisher;
    }

    /* ---------------- branch resolution ---------------- */

    private BranchResponse resolveBranch(String branchCode) {
        if (branchCode == null || branchCode.isBlank()) {
            return null;
        }
        String code = branchCode.trim();
        BranchResponse branch;
        try {
            branch = branchClient.getBranchById(code);
        } catch (feign.FeignException.NotFound ex) {
            throw new IllegalArgumentException(
                    "Preferred branch not found with code: '" + code + "'");
        }
        if (branch == null) {
            throw new IllegalArgumentException(
                    "Preferred branch not found with code: '" + code + "'");
        }
        if (!"ACTIVE".equalsIgnoreCase(branch.getStatus())) {
            throw new IllegalArgumentException(
                    "Preferred branch '" + code + "' is not ACTIVE (" + branch.getStatus() + ")");
        }
        return branch;
    }

    /* ---------------- submission ---------------- */

    @Transactional
    public KycResponse submitKyc(KycSubmitRequest request) {
        if (request == null || request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (request.getKycType() == null || request.getKycType().isBlank()) {
            throw new IllegalArgumentException("KYC type is required");
        }
        if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
            throw new IllegalArgumentException("At least one KYC document is required");
        }

        BranchResponse branch = resolveBranch(request.getPreferredBranchCode());
        LocalDateTime now = LocalDateTime.now();

        KycApplicationEntity application = newApplication(
                request.getCustomerId(),
                request.getKycType(),
                branch,
                now);

        List<KycDocumentEntity> documents = request.getDocuments().stream()
                .map(document -> newDocument(application.getKycId(), document, now))
                .toList();

        applicationRepository.save(application);
        documentRepository.saveAll(documents);
        return toResponse(application, documents);
    }

    @Transactional
    public KycResponse submitKycDocuments(String customerId, String kycType, String preferredBranchCode, String aadhaarNumber, String panNumber, MultipartFile aadhaarFile, MultipartFile panFile,String email,String name,String mobileNumber) throws IOException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (kycType == null || kycType.isBlank()) {
            throw new IllegalArgumentException("KYC type is required");
        }
        validateDocument(aadhaarFile, "Aadhaar");
        validateDocument(panFile, "PAN");
        String normalizedAadhaar = requireMatching(AADHAAR_PATTERN, aadhaarNumber, "Aadhaar number must contain 12 digits");
        String normalizedPan = requireMatching(PAN_PATTERN, panNumber, "PAN number must be in the format ABCDE1234F");

        // Only resolve if a code is provided - allow null branches
        BranchResponse branch = resolveBranch(preferredBranchCode);

        byte[] aadhaarContent = aadhaarFile.getBytes();
        byte[] panContent = panFile.getBytes();
        String aadhaarExtension = detectExtension(aadhaarContent);
        String panExtension = detectExtension(panContent);
        LocalDateTime now = LocalDateTime.now();

        KycApplicationEntity application = newApplication(customerId, kycType, branch, now);
        List<KycDocumentEntity> documents = List.of(
                newDocument(application.getKycId(), "AADHAAR", normalizedAadhaar, aadhaarExtension, now),
                newDocument(application.getKycId(), "PAN", normalizedPan, panExtension, now));

        KycApplicationEntity save = applicationRepository.save(application);
        documentRepository.saveAll(documents);
        documentContentRepository.saveAll(List.of(newDocumentContent(documents.get(0).getDocumentId(), aadhaarContent), newDocumentContent(documents.get(1).getDocumentId(), panContent)));

        //kafka
        KycEvent kycEvent=new KycEvent();
        kycEvent.setBranchName(branch.getBranchName());
        kycEvent.setName(name);
        kycEvent.setEmail(email);
        kycEvent.setCreatedAt(save.getCreatedAt());
        kycEvent.setVerificationStatus(save.getVerificationStatus());
        kycEvent.setKycId(save.getKycId());
        kycEventPublisher.kycEventPublish(kycEvent);

        return toResponse(application, documents);
    }

    private KycApplicationEntity newApplication(
            String customerId, String kycType, BranchResponse branch, LocalDateTime now) {
        KycApplicationEntity application = new KycApplicationEntity();
        application.setKycId(newId("KYC-"));
        application.setCustomerId(customerId);
        application.setKycType(kycType);
        if (branch != null) {
            application.setBranchCode(branch.getBranchCode());
            application.setBranchName(branch.getBranchName());
            application.setBranchIfsc(branch.getIfscCode());
        }
        application.setVerificationStatus("PENDING");
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        return application;
    }

    private KycDocumentEntity newDocument(String kycId, KycDocumentRequest request, LocalDateTime now) {
        return newDocument(kycId, request.getDocumentType(), request.getDocumentNumber(), request.getFileName(), now);
    }

    private KycDocumentEntity newDocument(String kycId, String type, String number, String fileName, LocalDateTime now) {
        KycDocumentEntity document = new KycDocumentEntity();
        document.setDocumentId(newId("DOC-"));
        document.setKycId(kycId);
        document.setDocumentType(type);
        document.setDocumentNumber(number);
        document.setFileName(fileName);
        document.setVerificationStatus("PENDING");
        document.setUploadedAt(now);
        return document;
    }

    private KycDocumentContentEntity newDocumentContent(String documentId, byte[] content) {
        KycDocumentContentEntity entity = new KycDocumentContentEntity();
        entity.setDocumentId(documentId);
        entity.setContent(content);
        return entity;
    }

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String requireMatching(Pattern pattern, String value, String message) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private void validateDocument(MultipartFile file, String documentName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(documentName + " document file is required");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(documentName + " document must not exceed 10 MB");
        }
    }

    private String detectExtension(byte[] content) {
        if (content.length >= 5
                && content[0] == '%' && content[1] == 'P' && content[2] == 'D'
                && content[3] == 'F' && content[4] == '-') {
            return ".pdf";
        }
        if (content.length >= 8
                && (content[0] & 0xff) == 0x89 && content[1] == 'P'
                && content[2] == 'N' && content[3] == 'G'
                && content[4] == 0x0d && content[5] == 0x0a
                && content[6] == 0x1a && content[7] == 0x0a) {
            return ".png";
        }
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        throw new IllegalArgumentException("Documents must be PDF, PNG, or JPEG files");
    }

    /* ---------------- read ---------------- */

    @Transactional(readOnly = true)
    public List<KycResponse> getKycByCustomerId(String customerId) {
        List<KycApplicationEntity> applications = applicationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return toResponses(applications);
    }

    @Transactional(readOnly = true)
    public List<KycResponse> getAllKyc() {
        return toResponses(applicationRepository.findAllByOrderByCreatedAtDesc());
    }

    private List<KycResponse> toResponses(List<KycApplicationEntity> applications) {
        if (applications.isEmpty()) return List.of();
        List<String> kycIds = applications.stream().map(KycApplicationEntity::getKycId).toList();
        List<KycDocumentEntity> documents = documentRepository.findAllByKycIdInOrderByUploadedAtDesc(kycIds);
        var documentsByKyc = documents.stream().collect(Collectors.groupingBy(KycDocumentEntity::getKycId));
        return applications.stream()
                .map(application -> toResponse(application, documentsByKyc.getOrDefault(application.getKycId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public KycDocumentFile getDocument(String kycId, String documentId) {
        KycDocumentEntity document = documentRepository.findByKycIdAndDocumentId(kycId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("KYC document not found for id: " + documentId));
        KycDocumentContentEntity content = documentContentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("KYC document content is unavailable"));
        return new KycDocumentFile(document.getFileName(), content.getContent());
    }

    /* ---------------- approval ---------------- */

    @Transactional
    public KycStatusResponse updateKycStatus(String kycId, KycStatusUpdateRequest request) {
        KycApplicationEntity current = applicationRepository.findById(kycId).orElseThrow(() -> new IllegalArgumentException("KYC record not found for id: " + kycId));
        if (request == null || request.getVerificationStatus() == null) {
            throw new IllegalArgumentException("Verification status is required");
        }

        String status = request.getVerificationStatus().trim().toUpperCase(Locale.ROOT);
        if (!List.of("PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Unsupported KYC verification status: " + status);
        }
        if ("REJECTED".equals(status)
                && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("A rejection reason is required");
        }

        KycAccountResponse account = null;

        if ("APPROVED".equals(status) && !"APPROVED".equals(current.getVerificationStatus())) {
            String branchCodeToUse = current.getBranchCode();

            if (branchCodeToUse == null || branchCodeToUse.isBlank()) {
                throw new IllegalStateException(
                        "Cannot approve this KYC: the application does not have a branch assigned. " +
                                "The customer must re-submit KYC with a valid branch selection."
                );
            }

            account = customerClient.openAccountAfterKycApproval(
                    current.getCustomerId(),
                    branchCodeToUse
            );
            customerClient.activateCustomer(current.getCustomerId());
        }

        current.setVerificationStatus(status);
        current.setReviewerId(request.getReviewerId());
        current.setRejectionReason(request.getRejectionReason());
        current.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(current);

        return KycStatusResponse.builder()
                .kycId(current.getKycId())
                .verificationStatus(current.getVerificationStatus())
                .reviewerId(current.getReviewerId())
                .rejectionReason(current.getRejectionReason())
                .updatedAt(current.getUpdatedAt())
                .account(account)
                .build();
    }

    /* ---------------- mapping ---------------- */

    private KycResponse toResponse(KycApplicationEntity application, List<KycDocumentEntity> documents) {
        List<KycDocumentResponse> documentResponses = new ArrayList<>(documents.size());
        for (KycDocumentEntity document : documents) {
            documentResponses.add(KycDocumentResponse.builder()
                    .documentId(document.getDocumentId())
                    .documentType(document.getDocumentType())
                    .documentNumber(document.getDocumentNumber())
                    .fileName(document.getFileName())
                    .verificationStatus(document.getVerificationStatus())
                    .uploadedAt(document.getUploadedAt())
                    .build());
        }
        return KycResponse.builder()
                .kycId(application.getKycId())
                .customerId(application.getCustomerId())
                .kycType(application.getKycType())
                .branchCode(application.getBranchCode())
                .branchName(application.getBranchName())
                .branchIfsc(application.getBranchIfsc())
                .verificationStatus(application.getVerificationStatus())
                .reviewerId(application.getReviewerId())
                .rejectionReason(application.getRejectionReason())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .documents(documentResponses)
                .build();
    }

    public Page<KycApplicationEntity> branchKyc(String branchCode, int page, int size) {
        Page<KycApplicationEntity> res = applicationRepository.findByBranchCode(branchCode, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return res;
    }
    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
}