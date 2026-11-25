package com.nova.bank.loan.service;

import com.nova.bank.loan.client.CustomerClient;
import com.nova.bank.loan.client.CustomerProfileResponse;
import com.nova.bank.loan.client.CustomerResponse;
import com.nova.bank.loan.client.KycClient;
import com.nova.bank.loan.client.KycResponse;
import com.nova.bank.loan.client.AccountClient;
import com.nova.bank.loan.dto.*;
import com.nova.bank.loan.entity.*;
import com.nova.bank.loan.exception.LoanDependencyException;
import com.nova.bank.loan.producer.LoanNotificationEvent;
import com.nova.bank.loan.producer.LoanNotificationPublisher;
import com.nova.bank.loan.repository.*;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LoanService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 2;

    private final LoanProductRepository productRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanDocumentRepository documentRepository;
    private final LoanAccountRepository accountRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanPaymentRepository paymentRepository;
    private final LoanApplicationHistoryRepository historyRepository;
    private final CustomerClient customerClient;
    private final KycClient kycClient;
    private final AccountClient accountClient;
    private final Path uploadDirectory;
    private final LoanNotificationPublisher notificationPublisher;

    @Autowired
    public LoanService(LoanProductRepository productRepository,
                       LoanApplicationRepository applicationRepository,
                       LoanDocumentRepository documentRepository,
                       LoanAccountRepository accountRepository,
                       RepaymentScheduleRepository scheduleRepository,
                       LoanPaymentRepository paymentRepository,
                       LoanApplicationHistoryRepository historyRepository,
                       CustomerClient customerClient,
                       KycClient kycClient,
                       AccountClient accountClient,
                       @Value("${loan.upload.dir}") String uploadDirectory,
                       LoanNotificationPublisher notificationPublisher) {
        this.productRepository = productRepository;
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.accountRepository = accountRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.customerClient = customerClient;
        this.kycClient = kycClient;
        this.accountClient = accountClient;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.notificationPublisher = notificationPublisher;
    }

    public LoanService(LoanProductRepository productRepository,
                       LoanApplicationRepository applicationRepository,
                       LoanDocumentRepository documentRepository,
                       LoanAccountRepository accountRepository,
                       RepaymentScheduleRepository scheduleRepository,
                       LoanPaymentRepository paymentRepository,
                       LoanApplicationHistoryRepository historyRepository,
                       CustomerClient customerClient,
                       KycClient kycClient,
                       AccountClient accountClient,
                       @Value("${loan.upload.dir}") String uploadDirectory) {
        this(productRepository, applicationRepository, documentRepository, accountRepository,
                scheduleRepository, paymentRepository, historyRepository, customerClient,
                kycClient, accountClient, uploadDirectory, null);
    }

    @Transactional
    public LoanProduct createProduct(CreateLoanProductRequest request) {
        if (request.minimumAmount().compareTo(request.maximumAmount()) > 0
                || request.minimumTenureMonths() > request.maximumTenureMonths()) {
            throw new IllegalArgumentException("Minimum values cannot be greater than maximum values");
        }
        LoanProduct product = new LoanProduct();
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setMinimumAmount(request.minimumAmount());
        product.setMaximumAmount(request.maximumAmount());
        product.setAnnualInterestRate(request.annualInterestRate());
        product.setMinimumTenureMonths(request.minimumTenureMonths());
        product.setMaximumTenureMonths(request.maximumTenureMonths());
        product.setMaximumFoirPercent(request.maximumFoirPercent());
        product.setMinimumCreditScore(request.minimumCreditScore());
        return productRepository.save(product);
    }

    public List<LoanProduct> getAvailableProducts() {
        return productRepository.findByActiveTrue();
    }

    public LoanEstimateResponse estimate(Long productId, LoanEstimateRequest request) {
        LoanProduct product = getProduct(productId);
        checkAmountAndTenure(product, request.amount(), request.tenureMonths());
        if (request.existingMonthlyEmi().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Existing EMI cannot be negative");
        }
        BigDecimal emi = calculateEmi(request.amount(), product.getAnnualInterestRate(), request.tenureMonths());
        // FOIR is the part of monthly income used for all monthly loan payments.
        BigDecimal maxEmi = request.monthlyIncome()
                .multiply(product.getMaximumFoirPercent())
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP)
                .subtract(request.existingMonthlyEmi())
                .max(ZERO);
        BigDecimal foir = emi.add(request.existingMonthlyEmi())
                .multiply(BigDecimal.valueOf(100))
                .divide(request.monthlyIncome(), MONEY_SCALE, RoundingMode.HALF_UP);
        return new LoanEstimateResponse(emi, foir, maxEmi,
                emi.compareTo(maxEmi) <= 0);
    }

    @Transactional
    public LoanApplication apply(ApplyForLoanRequest request) {
        CustomerResponse customer = getMyCustomer();
        LoanProduct product = getProduct(request.productId());
        checkAmountAndTenure(product, request.amount(), request.tenureMonths());
        if (customer.customerId() == null || customer.customerId().isBlank()) {
            throw new IllegalStateException("Customer Service returned an empty customer id");
        }
        List<KycResponse> kycRecords;
        try {
            kycRecords = kycClient.getCustomerKyc(customer.customerId());
        } catch (FeignException exception) {
            throw new LoanDependencyException("Could not verify KYC with KYC Service", exception);
        }
        boolean hasApprovedKyc = kycRecords != null && kycRecords.stream()
                .anyMatch(kyc -> "APPROVED".equalsIgnoreCase(kyc.verificationStatus()));
        if (!hasApprovedKyc) {
            throw new IllegalArgumentException("An approved KYC application is required before applying");
        }
        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID().toString());
        application.setCustomerId(customer.customerId());
        application.setCustomerName(customer.name());
        application.setCustomerEmail(customer.email());
        application.setCustomerPhone(customer.mobileNumber());
        application.setKycStatus("APPROVED");
        application.setProductId(product.getId());
        application.setRequestedAmount(request.amount());
        application.setTenureMonths(request.tenureMonths());
        application.setPurpose(request.purpose().trim());
        application.setStatus(LoanStatus.DRAFT);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "APPLICATION_STARTED", null, saved.getStatus(),
                "Customer started a loan application");
        return saved;
    }

    @Transactional
    public LoanApplication submitEmploymentInfo(String applicationId, EmploymentInfoRequest request) {
        LoanApplication application = getMyApplication(applicationId);
        if (application.getStatus() != LoanStatus.DRAFT
                && application.getStatus() != LoanStatus.SUBMITTED) {
            throw new IllegalArgumentException("Employment details can only be updated before application submission");
        }
        if (request.existingMonthlyEmi().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Existing EMI cannot be negative");
        }
        String employmentType = request.employmentType().trim().toUpperCase();
        if (employmentType.equals("SALARIED")) {
            if (request.designation() == null || request.designation().isBlank()
                    || request.workExperienceYears() == null || request.workExperienceYears() < 0) {
                throw new IllegalArgumentException(
                        "Salaried applicants must provide their designation and work experience");
            }
        } else if (employmentType.equals("SELF_EMPLOYED") || employmentType.equals("SELF-EMPLOYED")) {
            if (request.businessName() == null || request.businessName().isBlank()
                    || request.businessType() == null || request.businessType().isBlank()
                    || request.businessAddress() == null || request.businessAddress().isBlank()
                    || request.businessVintageYears() == null || request.businessVintageYears() < 0
                    || request.annualTurnover() == null
                    || request.annualTurnover().compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Self-employed applicants must provide business name, type, address, vintage, and annual turnover");
            }
        } else {
            throw new IllegalArgumentException("Employment type must be SALARIED or SELF_EMPLOYED");
        }
        application.setEmployerName(request.employerName().trim());
        application.setEmploymentType(request.employmentType().trim());
        application.setMonthlyIncome(request.monthlyIncome());
        application.setExistingMonthlyEmi(request.existingMonthlyEmi());
        application.setDesignation(trimToNull(request.designation()));
        application.setWorkExperienceYears(request.workExperienceYears());
        application.setBusinessName(trimToNull(request.businessName()));
        application.setBusinessType(trimToNull(request.businessType()));
        application.setBusinessVintageYears(request.businessVintageYears());
        application.setBusinessAddress(trimToNull(request.businessAddress()));
        application.setBusinessRegistrationNumber(trimToNull(request.businessRegistrationNumber()));
        application.setAnnualTurnover(request.annualTurnover());
        LoanStatus previousStatus = application.getStatus();
        if (previousStatus == LoanStatus.SUBMITTED) {
            application.setStatus(LoanStatus.DETAILS_SUBMITTED);
        }
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "EMPLOYMENT_DETAILS_SAVED", previousStatus,
                saved.getStatus(), "Customer submitted employment and income details");
        return saved;
    }

    @Transactional
    public LoanApplication submitApplication(String applicationId) {
        LoanApplication application = getMyApplication(applicationId);
        requireStatus(application, LoanStatus.DRAFT);
        if (application.getPurpose() == null || application.getPurpose().isBlank()
                || application.getEmploymentType() == null || application.getEmploymentType().isBlank()
                || application.getEmployerName() == null || application.getEmployerName().isBlank()
                || application.getMonthlyIncome() == null
                || application.getMonthlyIncome().compareTo(ZERO) <= 0
                || application.getExistingMonthlyEmi() == null
                || application.getExistingMonthlyEmi().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Complete loan and employment details before submitting");
        }
        String employmentType = application.getEmploymentType().trim().toUpperCase();
        if (employmentType.equals("SALARIED")) {
            if (application.getDesignation() == null || application.getDesignation().isBlank()
                    || application.getWorkExperienceYears() == null
                    || application.getWorkExperienceYears() < 0) {
                throw new IllegalArgumentException("Complete salaried employment details before submitting");
            }
        } else if (employmentType.equals("SELF_EMPLOYED") || employmentType.equals("SELF-EMPLOYED")) {
            if (application.getBusinessName() == null || application.getBusinessName().isBlank()
                    || application.getBusinessType() == null || application.getBusinessType().isBlank()
                    || application.getBusinessAddress() == null || application.getBusinessAddress().isBlank()
                    || application.getBusinessVintageYears() == null
                    || application.getBusinessVintageYears() < 0
                    || application.getAnnualTurnover() == null
                    || application.getAnnualTurnover().compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("Complete self-employed business details before submitting");
            }
        } else {
            throw new IllegalArgumentException("Employment type must be SALARIED or SELF_EMPLOYED");
        }
        List<String> uploadedTypes = documentRepository.findByApplicationId(applicationId).stream()
                .map(document -> document.getDocumentType().trim().toUpperCase().replace('-', '_'))
                .toList();
        List<String> missingTypes = new ArrayList<>();
        if (!uploadedTypes.contains("AADHAAR")) {
            missingTypes.add("AADHAAR");
        }
        if (!uploadedTypes.contains("PAN")) {
            missingTypes.add("PAN");
        }
        if (!uploadedTypes.contains("SALARY_SLIP") && !uploadedTypes.contains("BANK_STATEMENT")) {
            missingTypes.add("SALARY_SLIP or BANK_STATEMENT");
        }
        if (!missingTypes.isEmpty()) {
            throw new IllegalArgumentException("Required documents are missing: " + String.join(", ", missingTypes));
        }
        application.setStatus(LoanStatus.DETAILS_SUBMITTED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "APPLICATION_SUBMITTED", LoanStatus.DRAFT, saved.getStatus(),
                "Customer submitted loan application with required documents");
        publishLoanNotification(saved, "LOAN_APPLICATION_SUBMITTED", "SUBMITTED", saved.getRequestedAmount(),
                "We received your loan application and required documents. We will email you at every review stage.");
        return saved;
    }

    @Transactional
    public LoanDocumentResponse uploadDocument(String applicationId, String documentType, MultipartFile file) {
        LoanApplication application = getMyApplication(applicationId);
        if (application.getStatus() != LoanStatus.DRAFT
                && application.getStatus() != LoanStatus.DETAILS_SUBMITTED
                && application.getStatus() != LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED
                && application.getStatus() != LoanStatus.RETURNED_FOR_REVIEW) {
            throw new IllegalArgumentException("Documents can only be uploaded before or during application review");
        }
        if (documentType == null || documentType.isBlank() || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document type and a non-empty file are required");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Document size cannot be more than 10 MB");
        }
        String originalName = Path.of(file.getOriginalFilename() == null
                ? "document" : file.getOriginalFilename()).getFileName().toString();
        String storedName = UUID.randomUUID().toString();
        Path storedPath = uploadDirectory.resolve(storedName).normalize();
        if (!storedPath.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid document file name");
        }
        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(storedPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save uploaded document", exception);
        }
        LoanDocument document = new LoanDocument();
        document.setId(storedName);
        document.setApplicationId(applicationId);
        document.setDocumentType(documentType.trim());
        document.setOriginalFileName(originalName);
        document.setFilePath(storedPath.toString());
        LoanDocument saved = documentRepository.save(document);
        recordHistory(application, "DOCUMENT_UPLOADED", application.getStatus(), application.getStatus(),
                "Customer uploaded " + document.getDocumentType());
        return toDocumentResponse(saved);
    }

    public List<LoanDocumentResponse> getDocuments(String applicationId) {
        getApplication(applicationId);
        return documentRepository.findByApplicationId(applicationId).stream()
                .map(this::toDocumentResponse).toList();
    }

    public LoanApplication getApplicationForReview(String applicationId) {
        return getApplication(applicationId);
    }

    public LoanApplicationReviewResponse getApplicationReviewDetails(String applicationId) {
        LoanApplication application = getApplication(applicationId);
        CustomerProfileResponse customer;
        try {
            customer = customerClient.getCustomer(application.getCustomerId());
        } catch (FeignException exception) {
            throw new LoanDependencyException("Could not retrieve the applicant profile from Customer Service",
                    exception);
        }
        if (customer == null || !application.getCustomerId().equals(customer.customerId())) {
            throw new IllegalStateException("Customer Service returned a mismatched applicant profile");
        }
        List<LoanDocumentResponse> documents = documentRepository.findByApplicationId(applicationId).stream()
                .map(this::toDocumentResponse).toList();
        List<LoanApplicationHistory> history = historyRepository
                .findByApplicationIdOrderByCreatedAtAsc(applicationId);
        return new LoanApplicationReviewResponse(application, customer, documents, history);
    }

    public LoanApplication getMyApplicationDetails(String applicationId) {
        return getMyApplication(applicationId);
    }

    public Resource getDocumentFile(String documentId) {
        LoanDocument document = getDocument(documentId);
        try {
            Path file = Path.of(document.getFilePath()).normalize();
            if (!file.startsWith(uploadDirectory)) {
                throw new IllegalStateException("Saved document path is invalid");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Saved document file is missing");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Saved document path is invalid", exception);
        }
    }

    public Resource getMyDocumentFile(String documentId) {
        LoanDocument document = getDocument(documentId);
        getMyApplication(document.getApplicationId());
        return getDocumentFile(documentId);
    }

    public String getDocumentContentType(String documentId) {
        LoanDocument document = getDocument(documentId);
        String contentType = URLConnection.guessContentTypeFromName(document.getOriginalFileName());
        return contentType == null ? "application/octet-stream" : contentType;
    }

    @Transactional
    public void deleteDocument(String documentId) {
        LoanDocument document = getDocument(documentId);
        LoanApplication application = getApplication(document.getApplicationId());
        if (application.getStatus() != LoanStatus.SUBMITTED
                && application.getStatus() != LoanStatus.DETAILS_SUBMITTED
                && application.getStatus() != LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED
                && application.getStatus() != LoanStatus.RETURNED_FOR_REVIEW) {
            throw new IllegalArgumentException("Documents can only be verified while application documents are under review");
        }
        try {
            Path file = Path.of(document.getFilePath()).normalize();
            if (!file.startsWith(uploadDirectory)) {
                throw new IllegalStateException("Saved document path is invalid");
            }
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove rejected document", exception);
        }
        documentRepository.delete(document);
    }

    @Transactional
    public LoanDocumentResponse verifyDocument(String documentId, boolean verified, String employeeId) {
        LoanDocument document = getDocument(documentId);
        LoanApplication application = getApplication(document.getApplicationId());
        if (application.getStatus() != LoanStatus.SUBMITTED
                && application.getStatus() != LoanStatus.DETAILS_SUBMITTED
                && application.getStatus() != LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED
                && application.getStatus() != LoanStatus.RETURNED_FOR_REVIEW) {
            throw new IllegalArgumentException("Rejected documents can only be removed while documents are under review");
        }
        LoanStatus previousStatus = application.getStatus();
        document.setVerified(verified);
        document.setVerifiedBy(employeeId);
        LoanDocument savedDocument = documentRepository.save(document);
        List<LoanDocument> documents = documentRepository.findByApplicationId(application.getId());
        if (!documents.isEmpty() && documents.stream().allMatch(LoanDocument::isVerified)) {
            application.setStatus(LoanStatus.DOCUMENTS_VERIFIED);
            applicationRepository.save(application);
        }
        recordHistory(application, verified ? "DOCUMENT_VERIFIED" : "DOCUMENT_REJECTED",
                previousStatus, application.getStatus(), document.getDocumentType(), employeeId);
        return toDocumentResponse(savedDocument);
    }

    @Transactional
    public LoanApplication assessCredit(String applicationId, CreditAssessmentRequest request) {
        LoanApplication application = getApplication(applicationId);
        LoanStatus previousStatus = application.getStatus();
        if (previousStatus != LoanStatus.DOCUMENTS_VERIFIED
                && previousStatus != LoanStatus.RETURNED_FOR_REVIEW) {
            throw new IllegalArgumentException("Application must have verified documents before credit assessment");
        }
        List<LoanDocument> documents = documentRepository.findByApplicationId(applicationId);
        if (documents.isEmpty() || documents.stream().anyMatch(document -> !document.isVerified())) {
            throw new IllegalArgumentException("All supporting documents must be verified first");
        }
        List<String> uploadedTypes = documents.stream()
                .map(document -> document.getDocumentType().trim().toUpperCase().replace('-', '_'))
                .toList();
        List<String> missingTypes = getRequiredDocumentTypes(application).stream()
                .filter(required -> !uploadedTypes.contains(required))
                .toList();
        List<String> missingRequiredTypes = new ArrayList<>(missingTypes);
        if (!uploadedTypes.contains("SALARY_SLIP") && !uploadedTypes.contains("BANK_STATEMENT")) {
            missingRequiredTypes.add("SALARY_SLIP or BANK_STATEMENT");
        }
        if (!missingRequiredTypes.isEmpty()) {
            throw new IllegalArgumentException("Required documents are missing: "
                    + String.join(", ", missingRequiredTypes));
        }
        LoanProduct product = getProduct(application.getProductId());
        BigDecimal emi = calculateEmi(application.getRequestedAmount(),
                product.getAnnualInterestRate(), application.getTenureMonths());
        BigDecimal income = application.getMonthlyIncome();
        BigDecimal totalEmi = emi.add(application.getExistingMonthlyEmi());
        BigDecimal foir = totalEmi.multiply(BigDecimal.valueOf(100))
                .divide(income, MONEY_SCALE, RoundingMode.HALF_UP);
        boolean passed = request.creditScore() >= product.getMinimumCreditScore()
                && foir.compareTo(product.getMaximumFoirPercent()) <= 0;
        application.setCreditScore(request.creditScore());
        application.setAssessedFoirPercent(foir);
        application.setCreditPassed(passed);
        application.setStatus(passed ? LoanStatus.CREDIT_PASSED : LoanStatus.CREDIT_FAILED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "CREDIT_ASSESSED", previousStatus,
                saved.getStatus(), "Credit score and repayment capacity assessed");
        return saved;
    }

    @Transactional
    public LoanApplication approveAndCreateOffer(String applicationId) {
        LoanApplication application = getApplication(applicationId);
        requireStatus(application, LoanStatus.RECOMMENDED);
        if (!"APPROVE".equals(application.getRecommendation())) {
            throw new IllegalArgumentException("A loan officer approval recommendation is required");
        }
        application.setStatus(LoanStatus.OFFERED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "MANAGER_APPROVED", LoanStatus.RECOMMENDED, saved.getStatus(),
                "Branch manager approved the loan offer");
        publishLoanNotification(saved, "LOAN_APPROVED", "APPROVED", saved.getRecommendedAmount(),
                "Your loan application has been approved");
        return saved;
    }

    @Transactional
    public LoanApplication respondToOffer(String applicationId, boolean accepted) {
        LoanApplication application = getMyApplication(applicationId);
        requireStatus(application, LoanStatus.OFFERED);
        application.setOfferAccepted(accepted);
        if (!accepted) {
            application.setStatus(LoanStatus.OFFER_REJECTED);
            LoanApplication saved = applicationRepository.save(application);
            recordHistory(saved, "OFFER_REJECTED", LoanStatus.OFFERED, saved.getStatus(),
                    "Customer declined the loan offer");
            return saved;
        }
        LoanProduct product = getProduct(application.getProductId());
        BigDecimal offerAmount = application.getRecommendedAmount() == null
                ? application.getRequestedAmount() : application.getRecommendedAmount();
        BigDecimal offerRate = application.getRecommendedInterestRate() == null
                ? product.getAnnualInterestRate() : application.getRecommendedInterestRate();
        int offerTenure = application.getRecommendedTenureMonths() == null
                ? application.getTenureMonths() : application.getRecommendedTenureMonths();
        BigDecimal emi = calculateEmi(offerAmount, offerRate, offerTenure);
        application.setAgreementText("Loan agreement for application " + applicationId
                + ": principal " + offerAmount + ", annual interest rate "
                + offerRate + "%, tenure " + offerTenure
                + " months, monthly EMI " + emi + ".");
        application.setKeyFactStatement("KFS: loan amount " + offerAmount
                + "; annual interest rate " + offerRate
                + "%; tenure " + offerTenure + " months; EMI " + emi
                + "; total repayment " + emi.multiply(BigDecimal.valueOf(offerTenure))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP) + ".");
        application.setStatus(LoanStatus.OFFER_ACCEPTED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "OFFER_ACCEPTED", LoanStatus.OFFERED, saved.getStatus(),
                "Customer accepted the loan offer");
        publishLoanNotification(saved, "LOAN_OFFER_ACCEPTED", "OFFER_ACCEPTED", offerAmount,
                "You accepted the loan offer. Please review and accept the agreement.");
        return saved;
    }

    @Transactional
    public LoanApplication acceptAgreement(String applicationId) {
        LoanApplication application = getMyApplication(applicationId);
        requireStatus(application, LoanStatus.OFFER_ACCEPTED);
        application.setAgreementAccepted(true);
        application.setStatus(LoanStatus.AGREEMENT_ACCEPTED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "AGREEMENT_ACCEPTED", LoanStatus.OFFER_ACCEPTED, saved.getStatus(),
                "Customer accepted the loan agreement");
        publishLoanNotification(saved, "LOAN_AGREEMENT_ACCEPTED", "AGREEMENT_ACCEPTED", saved.getRecommendedAmount(),
                "You accepted the loan agreement. The branch will prepare and disburse the approved funds.");
        return saved;
    }

    @Transactional
    public LoanAccount prepareDisbursement(String applicationId) {
        LoanApplication application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + applicationId));
        LoanAccount existingAccount = accountRepository.findByApplicationId(applicationId).orElse(null);
        if (existingAccount != null) {
            return existingAccount;
        }
        LoanStatus previousStatus = application.getStatus();
        if (previousStatus != LoanStatus.AGREEMENT_ACCEPTED
                && previousStatus != LoanStatus.DISBURSEMENT_AUTHORIZED) {
            throw new IllegalArgumentException(
                    "Loan disbursement can only be prepared after agreement acceptance");
        }
        CustomerBankAccountResponse beneficiary = getDisbursementAccount(application.getCustomerId());
        LoanProduct product = getProduct(application.getProductId());
        BigDecimal principal = application.getRecommendedAmount() == null
                ? application.getRequestedAmount() : application.getRecommendedAmount();
        BigDecimal interestRate = application.getRecommendedInterestRate() == null
                ? product.getAnnualInterestRate() : application.getRecommendedInterestRate();
        int tenureMonths = application.getRecommendedTenureMonths() == null
                ? application.getTenureMonths() : application.getRecommendedTenureMonths();
        LoanAccount account = new LoanAccount();
        account.setId("LOAN-" + applicationId);
        account.setApplicationId(applicationId);
        account.setCustomerId(application.getCustomerId());
        account.setBankAccountNumber(beneficiary.accountNumber());
        account.setCurrency(beneficiary.currency());
        account.setPrincipal(principal);
        account.setOutstandingPrincipal(principal);
        account.setAnnualInterestRate(interestRate);
        account.setTenureMonths(tenureMonths);
        account.setMonthlyEmi(calculateEmi(principal, interestRate, tenureMonths));
        account.setDisbursedOn(LocalDate.now());
        account.setClosed(false);
        account.setStatus("PENDING_DISBURSEMENT");
        LoanAccount savedAccount = accountRepository.save(account);
        createSchedule(account, account.getPrincipal(), account.getTenureMonths(), LocalDate.now());
        application.setStatus(LoanStatus.DISBURSEMENT_PENDING);
        LoanApplication savedApplication = applicationRepository.save(application);
        recordHistory(savedApplication, "LOAN_ACCOUNT_CREATED", previousStatus,
                savedApplication.getStatus(),
                "Agreement and beneficiary account verified; loan account is pending disbursement");
        return savedAccount;
    }

    @Transactional
    public LoanAccount disburse(String applicationId) {
        LoanApplication application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + applicationId));
        requireStatus(application, LoanStatus.DISBURSEMENT_PENDING);
        LoanAccount account = accountRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException("Pending loan account has not been prepared"));
        if ("ACTIVE".equals(account.getStatus())) {
            return account;
        }
        if (!"PENDING_DISBURSEMENT".equals(account.getStatus())) {
            throw new IllegalStateException("Loan account is not pending disbursement");
        }
        LoanBalanceResponse confirmation = applyDisbursementToBankAccount(
                application, account.getBankAccountNumber(), account.getPrincipal());
        if (confirmation == null
                || !application.getCustomerId().equals(confirmation.customerId())
                || !account.getBankAccountNumber().equals(confirmation.accountNumber())
                || !account.getCurrency().equals(confirmation.currency())) {
            throw new IllegalStateException(
                    "Account Service returned an invalid loan transfer confirmation");
        }
        account.setStatus("ACTIVE");
        account.setDisbursedOn(LocalDate.now());
        LoanAccount activatedAccount = accountRepository.save(account);
        application.setStatus(LoanStatus.DISBURSED);
        LoanApplication savedApplication = applicationRepository.save(application);
        recordHistory(savedApplication, "LOAN_DISBURSED", LoanStatus.DISBURSEMENT_PENDING,
                application.getStatus(), "Loan funds disbursed to the customer's bank account");
        publishLoanNotification(savedApplication, "LOAN_DISBURSED", "DISBURSED", account.getPrincipal(),
                "Your loan has been disbursed successfully");
        return activatedAccount;
    }

    public LoanAccount getLoanAccountForApplication(String applicationId) {
        getApplication(applicationId);
        return accountRepository.findByApplicationId(applicationId).orElse(null);
    }

    private CustomerBankAccountResponse getDisbursementAccount(String customerId) {
        List<CustomerBankAccountResponse> accounts;
        try {
            accounts = accountClient.getCustomerAccounts(customerId);
        } catch (FeignException exception) {
            throw new LoanDependencyException(
                    "Could not verify the customer's beneficiary bank account", exception);
        }
        return accounts.stream()
                .filter(account -> customerId.equals(account.customerId()))
                .filter(account -> "ACTIVE".equals(account.accountStatus()))
                .filter(account -> "SAVINGS".equals(account.accountType())
                        || "CURRENT".equals(account.accountType()))
                .sorted(Comparator.comparingInt(account ->
                        "SAVINGS".equals(account.accountType()) ? 0 : 1))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer does not have an active savings or current beneficiary account"));
    }

    public List<LoanApplication> getMyApplications() {
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(getMyCustomerId());
    }

    public List<LoanApplication> getApplicationsForStaff() {
        return applicationRepository.findAllNewestFirst().stream()
                .filter(application -> application.getStatus() != LoanStatus.DRAFT)
                .toList();
    }

    public List<LoanApplication> getAssignedApplications(String officerId) {
        return applicationRepository.findOfficerInbox(officerId, List.of(
                LoanStatus.SUBMITTED,
                LoanStatus.DETAILS_SUBMITTED,
                LoanStatus.DOCUMENTS_VERIFIED,
                LoanStatus.CREDIT_PASSED,
                LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED,
                LoanStatus.RETURNED_FOR_REVIEW,
                LoanStatus.AGREEMENT_ACCEPTED,
                LoanStatus.DISBURSEMENT_AUTHORIZED,
                LoanStatus.DISBURSEMENT_PENDING));
    }

    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> statistics = new java.util.LinkedHashMap<>();
        for (LoanStatus status : LoanStatus.values()) {
            if (status != LoanStatus.DRAFT) {
                statistics.put(status.name(), applicationRepository.countByStatus(status));
            }
        }
        statistics.put("TOTAL", applicationRepository.count() - applicationRepository.countByStatus(LoanStatus.DRAFT));
        return statistics;
    }

    public List<LoanApplicationHistory> getApplicationHistory(String applicationId) {
        LoanApplication application = getApplication(applicationId);
        return historyRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId());
    }

    @Transactional
    public LoanApplication recommend(String applicationId, LoanRecommendationRequest request, String actorId) {
        LoanApplication application = getApplication(applicationId);
        requireStatus(application, LoanStatus.CREDIT_PASSED);
        String recommendation = request.recommendation().trim().toUpperCase();
        if (!List.of("APPROVE", "REJECT").contains(recommendation)) {
            throw new IllegalArgumentException("Recommendation must be APPROVE or REJECT");
        }
        if ("APPROVE".equals(recommendation)) {
            if (request.amount() == null || request.annualInterestRate() == null
                    || request.tenureMonths() == null) {
                throw new IllegalArgumentException("Approval recommendations must include amount, rate, and tenure");
            }
            LoanProduct product = getProduct(application.getProductId());
            if (request.amount().compareTo(application.getRequestedAmount()) > 0) {
                throw new IllegalArgumentException("Recommended amount cannot exceed the requested amount");
            }
            checkAmountAndTenure(product, request.amount(), request.tenureMonths());
            if (request.annualInterestRate().compareTo(product.getAnnualInterestRate()) < 0) {
                throw new IllegalArgumentException("Recommended interest rate cannot be below the product rate");
            }
            BigDecimal proposedEmi = calculateEmi(request.amount(), request.annualInterestRate(),
                    request.tenureMonths());
            BigDecimal proposedFoir = proposedEmi.add(application.getExistingMonthlyEmi())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(application.getMonthlyIncome(), MONEY_SCALE, RoundingMode.HALF_UP);
            if (proposedFoir.compareTo(product.getMaximumFoirPercent()) > 0) {
                throw new IllegalArgumentException("Recommended offer exceeds the product repayment-capacity limit");
            }
            application.setRecommendedAmount(request.amount());
            application.setRecommendedInterestRate(request.annualInterestRate());
            application.setRecommendedTenureMonths(request.tenureMonths());
        }
        application.setRecommendation(recommendation);
        application.setRecommendationRemarks(request.remarks().trim());
        application.setStatus(LoanStatus.RECOMMENDED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "LOAN_RECOMMENDED", LoanStatus.CREDIT_PASSED, saved.getStatus(),
                recommendation + ": " + request.remarks().trim(), actorId);
        return saved;
    }

    @Transactional
    public LoanApplication decideAsManager(String applicationId, BranchManagerDecisionRequest request,
                                           String actorId) {
        LoanApplication application = getApplication(applicationId);
        requireStatus(application, LoanStatus.RECOMMENDED);
        String decision = request.decision().trim().toUpperCase();
        if (!List.of("APPROVE", "REJECT", "RETURN").contains(decision)) {
            throw new IllegalArgumentException("Decision must be APPROVE, REJECT, or RETURN");
        }
        if ("APPROVE".equals(decision)) {
            if (!"APPROVE".equals(application.getRecommendation())) {
                throw new IllegalArgumentException("A rejected loan officer recommendation cannot be approved");
            }
            LoanProduct product = getProduct(application.getProductId());
            BigDecimal approvedAmount = request.amount() == null
                    ? application.getRecommendedAmount() : request.amount();
            BigDecimal approvedRate = request.annualInterestRate() == null
                    ? application.getRecommendedInterestRate() : request.annualInterestRate();
            Integer approvedTenure = request.tenureMonths() == null
                    ? application.getRecommendedTenureMonths() : request.tenureMonths();
            if (approvedAmount == null || approvedRate == null || approvedTenure == null) {
                throw new IllegalArgumentException("The loan officer recommendation must include proposed terms");
            }
            if (approvedAmount.compareTo(application.getRequestedAmount()) > 0) {
                throw new IllegalArgumentException("Approved amount cannot exceed the customer's requested amount");
            }
            checkAmountAndTenure(product, approvedAmount, approvedTenure);
            if (approvedRate.compareTo(product.getAnnualInterestRate()) < 0) {
                throw new IllegalArgumentException("Approved rate cannot be below the product rate");
            }
            BigDecimal approvedEmi = calculateEmi(approvedAmount, approvedRate, approvedTenure);
            BigDecimal approvedFoir = approvedEmi.add(application.getExistingMonthlyEmi())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(application.getMonthlyIncome(), MONEY_SCALE, RoundingMode.HALF_UP);
            if (approvedFoir.compareTo(product.getMaximumFoirPercent()) > 0) {
                throw new IllegalArgumentException("Approved offer exceeds the product repayment-capacity limit");
            }
            application.setRecommendedAmount(approvedAmount);
            application.setRecommendedInterestRate(approvedRate);
            application.setRecommendedTenureMonths(approvedTenure);
            application.setStatus(LoanStatus.OFFERED);
        } else if ("REJECT".equals(decision)) {
            application.setStatus(LoanStatus.REJECTED);
        } else {
            application.setStatus(LoanStatus.RETURNED_FOR_REVIEW);
        }
        application.setManagerDecisionReason(request.reason().trim());
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "MANAGER_" + decision, LoanStatus.RECOMMENDED, saved.getStatus(),
                request.reason().trim(), actorId);
        if ("APPROVE".equals(decision)) {
            publishLoanNotification(saved, "LOAN_APPROVED", "APPROVED", saved.getRecommendedAmount(),
                    "Your loan was approved by the branch manager. The approved offer is now available to accept.");
        } else if ("REJECT".equals(decision)) {
            publishLoanNotification(saved, "LOAN_REJECTED", "REJECTED", saved.getRequestedAmount(),
                    "Your loan application was rejected: " + request.reason().trim());
        } else {
            publishLoanNotification(saved, "LOAN_RETURNED", "RETURNED_FOR_REVIEW",
                    saved.getRequestedAmount(), "Your application was returned for review: " + request.reason().trim());
        }
        return saved;
    }

    @Transactional
    public LoanApplication assignApplication(String applicationId, LoanAssignmentRequest request, String actorId) {
        LoanApplication application = getApplication(applicationId);
        String previousOfficer = application.getAssignedOfficerId();
        application.setAssignedOfficerId(request.officerId().trim());
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "APPLICATION_ASSIGNED", application.getStatus(), application.getStatus(),
                "Assigned to " + request.officerId().trim()
                        + (previousOfficer == null ? "" : "; reassigned from " + previousOfficer), actorId);
        return saved;
    }

    @Transactional
    public LoanApplication requestAdditionalDocuments(String applicationId, AdditionalDocumentsRequest request,
                                                       String actorId) {
        LoanApplication application = getApplication(applicationId);
        if (application.getStatus() != LoanStatus.DETAILS_SUBMITTED
                && application.getStatus() != LoanStatus.DOCUMENTS_VERIFIED
                && application.getStatus() != LoanStatus.RETURNED_FOR_REVIEW) {
            throw new IllegalArgumentException("Additional documents cannot be requested at this application stage");
        }
        LoanStatus previousStatus = application.getStatus();
        application.setAdditionalDocumentsRequested(request.requestedDocuments().trim());
        application.setStatus(LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED);
        LoanApplication saved = applicationRepository.save(application);
        recordHistory(saved, "ADDITIONAL_DOCUMENTS_REQUESTED", previousStatus, saved.getStatus(),
                request.requestedDocuments().trim(), actorId);
        publishLoanNotification(saved, "ADDITIONAL_DOCUMENTS_REQUESTED", "DOCUMENTS_REQUIRED",
                saved.getRequestedAmount(), "Additional documents are required: " + request.requestedDocuments().trim());
        return saved;
    }

    public List<LoanAccount> getMyAccounts() {
        return accountRepository.findByCustomerId(getMyCustomerId());
    }

    public List<RepaymentSchedule> getMySchedule(String accountId) {
        LoanAccount account = getMyAccount(accountId);
        return scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber(account.getId());
    }

    public List<LoanPayment> getMyPayments(String accountId) {
        LoanAccount account = getMyAccount(accountId);
        return paymentRepository.findByLoanAccountIdOrderByPaidAtDesc(account.getId());
    }

    @Transactional
    public LoanAccount payEmi(String accountId, String paymentId, BigDecimal amount) {
        LoanAccount account = getMyAccountForUpdate(accountId);
        if (returnExistingPayment(account, paymentId, "EMI", amount)) {
            return account;
        }
        ensureOpen(account);
        RepaymentSchedule nextPayment = scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber(accountId).stream().filter(row -> !"PAID".equals(row.getStatus())).findFirst().orElseThrow(() -> new IllegalArgumentException("There are no unpaid installments"));
        BigDecimal remainingDue = nextPayment.getAmountDue().subtract(nextPayment.getAmountPaid());
        if (amount.compareTo(remainingDue) > 0) {
            throw new IllegalArgumentException("Monthly payment cannot exceed the remaining EMI due");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        applyPayment(account, nextPayment, paymentId, amount, "EMI");
        LoanApplication application = getApplication(account.getApplicationId());
        publishLoanNotification(application, "EMI_PAYMENT_SUCCESS", "PAID", amount,
                "Your EMI payment was successful");
        return accountRepository.save(account);
    }

    @Transactional
    public LoanAccount prepay(String accountId, String paymentId, BigDecimal amount) {
        LoanAccount account = getMyAccountForUpdate(accountId);
        if (returnExistingPayment(account, paymentId, "PREPAYMENT", amount)) {
            return account;
        }
        ensureOpen(account);
        if (amount.compareTo(account.getOutstandingPrincipal()) > 0) {
            throw new IllegalArgumentException("Prepayment cannot exceed outstanding principal");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(amount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        recordPayment(account, paymentId, amount, "PREPAYMENT");
        rebuildRemainingSchedule(account);
        closeIfPaid(account);
        return accountRepository.save(account);
    }

    public ForeclosureQuote getForeclosureQuote(String accountId) {
        LoanAccount account = getMyAccount(accountId);
        ensureOpen(account);
        return new ForeclosureQuote(accountId, account.getOutstandingPrincipal());
    }

    @Transactional
    public LoanAccount foreclose(String accountId, String paymentId, BigDecimal amount) {
        LoanAccount account = getMyAccountForUpdate(accountId);
        if (returnExistingPayment(account, paymentId, "FORECLOSURE", amount)) {
            return account;
        }
        ensureOpen(account);
        if (amount.compareTo(account.getOutstandingPrincipal()) != 0) {
            throw new IllegalArgumentException("Foreclosure amount must match the current quote");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        account.setOutstandingPrincipal(ZERO.setScale(MONEY_SCALE));
        recordPayment(account, paymentId, amount, "FORECLOSURE");
        closeIfPaid(account);
        return accountRepository.save(account);
    }

    private void applyPayment(LoanAccount account, RepaymentSchedule schedule,
                              String paymentId, BigDecimal amount, String type) {
        BigDecimal interestLeft = schedule.getInterestDue().subtract(schedule.getAmountPaid())
                .max(ZERO);
        BigDecimal interestPaid = amount.min(interestLeft);
        BigDecimal principalPaid = amount.subtract(interestPaid);
        schedule.setAmountPaid(schedule.getAmountPaid().add(amount));
        if (schedule.getAmountPaid().compareTo(schedule.getAmountDue()) >= 0) {
            schedule.setStatus("PAID");
        }
        scheduleRepository.save(schedule);
        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(principalPaid)
                .max(ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        recordPayment(account, paymentId, amount, type);
        closeIfPaid(account);
    }

    private void recordPayment(LoanAccount account, String paymentId, BigDecimal amount, String type) {
        LoanPayment payment = new LoanPayment();
        payment.setId(paymentRecordId(account, paymentId));
        payment.setLoanAccountId(account.getId());
        payment.setPaymentType(type);
        payment.setAmount(amount);
        payment.setOutstandingAfterPayment(account.getOutstandingPrincipal());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private String paymentOperationId(LoanAccount account, String paymentId) {
        String uniqueValue = account.getId() + ":" + paymentId;
        return "LOAN-PAYMENT-" + UUID.nameUUIDFromBytes(uniqueValue.getBytes(StandardCharsets.UTF_8));
    }

    private String paymentRecordId(LoanAccount account, String paymentId) {
        return paymentOperationId(account, paymentId);
    }

    private void closeIfPaid(LoanAccount account) {
        if (account.getOutstandingPrincipal().compareTo(ZERO) <= 0) {
            account.setOutstandingPrincipal(ZERO.setScale(MONEY_SCALE));
            account.setClosed(true);
            scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber(account.getId())
                    .stream().filter(row -> !"PAID".equals(row.getStatus()))
                    .forEach(row -> row.setStatus("CLOSED"));
            LoanApplication application = getApplication(account.getApplicationId());
            application.setStatus(LoanStatus.CLOSED);
            applicationRepository.save(application);
        }
    }

    private void rebuildRemainingSchedule(LoanAccount account) {
        List<RepaymentSchedule> rows = scheduleRepository
                .findByLoanAccountIdOrderByInstallmentNumber(account.getId());
        int nextInstallment = rows.stream().filter(row -> "PAID".equals(row.getStatus())).toList().size() + 1;
        int remainingMonths = account.getTenureMonths() - nextInstallment + 1;
        rows.stream().filter(row -> !"PAID".equals(row.getStatus())).forEach(scheduleRepository::delete);
        if (remainingMonths > 0 && account.getOutstandingPrincipal().compareTo(ZERO) > 0) {
            createSchedule(account, account.getOutstandingPrincipal(), remainingMonths,
                    LocalDate.now());
            List<RepaymentSchedule> newRows = scheduleRepository
                    .findByLoanAccountIdOrderByInstallmentNumber(account.getId());
            int installment = nextInstallment;
            for (RepaymentSchedule row : newRows) {
                if (!"PAID".equals(row.getStatus())) {
                    row.setInstallmentNumber(installment++);
                }
            }
            scheduleRepository.saveAll(newRows);
        }
    }

    private void createSchedule(LoanAccount account, BigDecimal principal,
                                int months, LocalDate disbursementDate) {
        BigDecimal rate = account.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(1200), 12, RoundingMode.HALF_UP);
        BigDecimal emi = calculateEmi(principal, account.getAnnualInterestRate(), months);
        BigDecimal balance = principal;
        List<RepaymentSchedule> rows = new ArrayList<>();
        for (int month = 1; month <= months; month++) {
            BigDecimal interest = balance.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal principalDue = emi.subtract(interest).max(ZERO);
            if (month == months || principalDue.compareTo(balance) > 0) {
                principalDue = balance;
            }
            BigDecimal totalDue = principalDue.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            RepaymentSchedule row = new RepaymentSchedule();
            row.setLoanAccountId(account.getId());
            row.setInstallmentNumber(month);
            row.setDueDate(disbursementDate.plusMonths(month));
            row.setPrincipalDue(principalDue.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            row.setInterestDue(interest);
            row.setAmountDue(totalDue);
            row.setAmountPaid(ZERO.setScale(MONEY_SCALE));
            row.setStatus("PENDING");
            rows.add(row);
            balance = balance.subtract(principalDue);
        }
        scheduleRepository.saveAll(rows);
        account.setMonthlyEmi(emi);
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        // Convert the annual percentage rate into a monthly decimal interest rate.
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 12, RoundingMode.HALF_UP);
        if (monthlyRate.compareTo(ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        // Standard fixed monthly payment formula, using decimal arithmetic for money.
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        return principal.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void checkAmountAndTenure(LoanProduct product, BigDecimal amount, int months) {
        if (amount.compareTo(product.getMinimumAmount()) < 0
                || amount.compareTo(product.getMaximumAmount()) > 0) {
            throw new IllegalArgumentException("Loan amount is outside this product's limits");
        }
        if (months < product.getMinimumTenureMonths()
                || months > product.getMaximumTenureMonths()) {
            throw new IllegalArgumentException("Loan tenure is outside this product's limits");
        }
    }

    private LoanProduct getProduct(Long id) {
        return productRepository.findById(id)
                .filter(LoanProduct::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active loan product not found: " + id));
    }

    private LoanApplication getApplication(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + id));
    }

    private LoanApplication getMyApplication(String id) {
        LoanApplication application = getApplication(id);
        if (!application.getCustomerId().equals(getMyCustomerId())) {
            throw new IllegalArgumentException("Loan application does not belong to the signed-in customer");
        }
        return application;
    }

    private LoanAccount getMyAccount(String id) {
        LoanAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + id));
        if (!account.getCustomerId().equals(getMyCustomerId())) {
            throw new IllegalArgumentException("Loan account does not belong to the signed-in customer");
        }
        return account;
    }

    private LoanAccount getMyAccountForUpdate(String id) {
        LoanAccount account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + id));
        if (!account.getCustomerId().equals(getMyCustomerId())) {
            throw new IllegalArgumentException("Loan account does not belong to the signed-in customer");
        }
        if (account.getStatus() != null && !account.getStatus().isBlank()
                && !"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Loan repayments are available after successful disbursement");
        }
        return account;
    }

    private boolean returnExistingPayment(LoanAccount account, String paymentId,
                                          String paymentType, BigDecimal amount) {
        LoanPayment existing = paymentRepository.findByIdAndLoanAccountId(paymentRecordId(account, paymentId), account.getId())
                .orElse(null);
        if (existing == null) {
            return false;
        }
        if (!existing.getPaymentType().equals(paymentType)
                || existing.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Payment ID was already used for a different payment");
        }
        return true;
    }

    private LoanBalanceResponse applyDisbursementToBankAccount(LoanApplication application,
                                                                String beneficiaryAccountNumber,
                                                                BigDecimal amount) {
        LoanBalanceRequest request = new LoanBalanceRequest("LOAN-DISBURSE-" + application.getId(),
                application.getCustomerId(),
                beneficiaryAccountNumber,
                amount);
        try {
            return accountClient.disburseLoan(request);
        } catch (FeignException exception) {
            throw new LoanDependencyException(
                    "Could not deposit loan funds into the customer's bank account", exception);
        }
    }

    private void collectPaymentFromBankAccount(LoanAccount account, String paymentId, BigDecimal amount) {
        LoanBalanceRequest request = new LoanBalanceRequest(paymentOperationId(account, paymentId), account.getCustomerId(), account.getBankAccountNumber(), amount);
        try {
            accountClient.collectLoanPayment(request);
        } catch (FeignException.BadRequest exception) {
            throw new IllegalArgumentException("Bank account declined the payment. Check that the account is active and has enough money.");
        } catch (FeignException exception) {
            throw new LoanDependencyException("Could not collect payment from the customer's bank account", exception);
        }
    }

    private LoanDocument getDocument(String id) {
        return documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Loan document not found: " + id));
    }

    private CustomerResponse getMyCustomer() {
        try {
            return customerClient.getMyProfile();
        } catch (FeignException exception) {
            throw new LoanDependencyException("Could not retrieve your profile from Customer Service", exception);
        }
    }

    private String getMyCustomerId() {
        CustomerResponse customer = getMyCustomer();
        if (customer.customerId() == null || customer.customerId().isBlank()) {
            throw new IllegalStateException("Customer Service returned an empty customer id");
        }
        return customer.customerId();
    }

    private LoanDocumentResponse toDocumentResponse(LoanDocument document) {
        return new LoanDocumentResponse(document.getId(), document.getApplicationId(),
                document.getDocumentType(), document.getOriginalFileName(), document.isVerified(),
                document.getVerifiedBy(), document.getUploadedAt());
    }

    private void recordHistory(LoanApplication application, String action, LoanStatus previousStatus, LoanStatus currentStatus, String remarks) {
        recordHistory(application, action, previousStatus, currentStatus, remarks, null);
    }

    private void recordHistory(LoanApplication application, String action, LoanStatus previousStatus, LoanStatus currentStatus, String remarks, String actorId) {
        LoanApplicationHistory history = new LoanApplicationHistory();
        history.setId(UUID.randomUUID().toString());
        history.setApplicationId(application.getId());
        history.setAction(action);
        history.setActorId(actorId);
        history.setPreviousStatus(previousStatus == null ? null : previousStatus.name());
        history.setCurrentStatus(currentStatus == null ? null : currentStatus.name());
        history.setRemarks(remarks);
        historyRepository.save(history);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> getRequiredDocumentTypes(LoanApplication application) {
        return List.of("AADHAAR", "PAN");
    }

    private void requireStatus(LoanApplication application, LoanStatus expected) {
        if (application.getStatus() != expected) {
            throw new IllegalArgumentException("Application must be in " + expected + " status");
        }
    }

    private void ensureOpen(LoanAccount account) {
        if (account.isClosed()) {
            throw new IllegalArgumentException("Loan account is already closed");
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void publishPendingNotifications() {
        // no-op
    }

    public void publishNotification(String applicationId, String customerId, LoanStatus status) {
        // no-op
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendEmiReminders() {
        if (notificationPublisher == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<RepaymentSchedule> dueSoon = scheduleRepository
                .findByDueDateBetweenAndStatusNot(today, today.plusDays(3), "PAID");
        for (RepaymentSchedule schedule : dueSoon) {
            LoanAccount account = accountRepository.findById(schedule.getLoanAccountId()).orElse(null);
            if (account == null || account.isClosed() || !"ACTIVE".equals(account.getStatus())) {
                continue;
            }
            LoanApplication application = applicationRepository.findById(account.getApplicationId()).orElse(null);
            if (application == null || application.getCustomerEmail() == null) {
                continue;
            }
            LoanNotificationEvent event = new LoanNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType("EMI_REMINDER");
            event.setApplicationId(account.getApplicationId());
            event.setLoanAccountId(account.getId());
            event.setCustomerId(account.getCustomerId());
            event.setCustomerName(application.getCustomerName());
            event.setEmail(application.getCustomerEmail());
            event.setAmount(schedule.getAmountDue().subtract(schedule.getAmountPaid()));
            event.setDueDate(schedule.getDueDate());
            event.setStatus("PENDING");
            event.setMessage("Your EMI is due on " + schedule.getDueDate());
            notificationPublisher.publish(event);
        }
    }

    private void publishLoanNotification(LoanApplication application, String eventType,
                                         String status, BigDecimal amount, String message) {
        if (notificationPublisher == null || application.getCustomerEmail() == null) {
            return;
        }
        LoanNotificationEvent event = new LoanNotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setApplicationId(application.getId());
        event.setCustomerId(application.getCustomerId());
        event.setCustomerName(application.getCustomerName());
        event.setEmail(application.getCustomerEmail());
        event.setAmount(amount);
        event.setStatus(status);
        event.setMessage(message);
        notificationPublisher.publish(event);
    }

    public List<LoanAccount> getAllLoanAccountsForStaff() {
        return accountRepository.findAll();
    }

    @Transactional
    public List<LoanPortfolioResponse> getLoanPortfolioForStaff() {
        LocalDate today = LocalDate.now();
        List<LoanPortfolioResponse> portfolio = new ArrayList<>();
        for (LoanAccount account : accountRepository.findAll()) {
            List<RepaymentSchedule> schedules = scheduleRepository
                    .findByLoanAccountIdOrderByInstallmentNumber(account.getId());
            for (RepaymentSchedule schedule : schedules) {
                if (!"PAID".equals(schedule.getStatus())
                        && schedule.getDueDate().isBefore(today)
                        && !"OVERDUE".equals(schedule.getStatus())) {
                    schedule.setStatus("OVERDUE");
                    scheduleRepository.save(schedule);
                }
            }
            List<LoanPayment> payments = paymentRepository
                    .findByLoanAccountIdOrderByPaidAtDesc(account.getId());
            BigDecimal totalPaid = payments.stream()
                    .map(LoanPayment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            RepaymentSchedule next = schedules.stream()
                    .filter(schedule -> !"PAID".equals(schedule.getStatus()))
                    .min(Comparator.comparing(RepaymentSchedule::getDueDate)
                            .thenComparing(RepaymentSchedule::getInstallmentNumber))
                    .orElse(null);
            LoanApplication application = applicationRepository.findById(account.getApplicationId()).orElse(null);
            portfolio.add(new LoanPortfolioResponse(
                    account.getId(),
                    account.getApplicationId(),
                    account.getCustomerId(),
                    application == null ? null : application.getCustomerName(),
                    account.getStatus(),
                    account.getPrincipal(),
                    account.getOutstandingPrincipal(),
                    account.getMonthlyEmi(),
                    next == null ? null : next.getDueDate(),
                    next == null ? null : next.getAmountDue().subtract(next.getAmountPaid()),
                    schedules.stream().filter(schedule -> "PENDING".equals(schedule.getStatus())).count(),
                    schedules.stream().filter(schedule -> "OVERDUE".equals(schedule.getStatus())).count(),
                    schedules.stream().filter(schedule -> "PAID".equals(schedule.getStatus())).count(),
                    totalPaid,
                    payments.isEmpty() ? null : payments.get(0).getPaidAt()
            ));
        }
        return portfolio;
    }

    public LoanAccount getLoanAccountForStaff(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found"));
    }

    public List<RepaymentSchedule> getScheduleForStaff(String accountId) {
        getLoanAccountForStaff(accountId);
        return scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber(accountId);
    }

    public List<LoanPayment> getPaymentsForStaff(String accountId) {
        getLoanAccountForStaff(accountId);
        return paymentRepository.findByLoanAccountIdOrderByPaidAtDesc(accountId);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<RepaymentSchedule> overdue = scheduleRepository
                .findByDueDateBeforeAndStatus(today, "PENDING");
        if (!overdue.isEmpty()) {
            overdue.forEach(schedule -> schedule.setStatus("OVERDUE"));
            scheduleRepository.saveAll(overdue);
        }
    }


    // new
    @Transactional
    public LoanAccount staffPayEmi(String accountId, String paymentId, BigDecimal amount, String actorId) {
        LoanAccount account = getAccountForStaffAction(accountId);
        if (returnExistingPayment(account, paymentId, "EMI", amount))
            return account;
        ensureOpen(account);
        RepaymentSchedule nextPayment = scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber(accountId).stream().filter(row -> !"PAID".equals(row.getStatus())).findFirst().orElseThrow(() -> new IllegalArgumentException("There are no unpaid installments"));
        BigDecimal remainingDue = nextPayment.getAmountDue().subtract(nextPayment.getAmountPaid());
        if (amount.compareTo(remainingDue) > 0) {throw new IllegalArgumentException("Payment cannot exceed the remaining EMI due");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        applyPayment(account, nextPayment, paymentId, amount, "EMI");
        LoanApplication application = getApplication(account.getApplicationId());
        recordHistory(application, "STAFF_EMI_PAYMENT", application.getStatus(), application.getStatus(), "Staff collected EMI payment of " + amount, actorId);
        publishLoanNotification(application, "EMI_PAYMENT_SUCCESS", "PAID", amount, "An EMI payment was recorded on your loan by bank staff");
        return accountRepository.save(account);
    }

    @Transactional
    public LoanAccount staffPrepay(String accountId, String paymentId, BigDecimal amount, String actorId) {
        LoanAccount account = getAccountForStaffAction(accountId);
        if (returnExistingPayment(account, paymentId, "PREPAYMENT", amount)) return account;
        ensureOpen(account);
        if (amount.compareTo(account.getOutstandingPrincipal()) > 0) {
            throw new IllegalArgumentException("Prepayment cannot exceed outstanding principal");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        account.setOutstandingPrincipal(account.getOutstandingPrincipal().subtract(amount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        recordPayment(account, paymentId, amount, "PREPAYMENT");
        rebuildRemainingSchedule(account);
        closeIfPaid(account);
        LoanApplication application = getApplication(account.getApplicationId());
        recordHistory(application, "STAFF_PREPAYMENT", application.getStatus(), application.getStatus(),
                "Staff recorded prepayment of " + amount, actorId);
        return accountRepository.save(account);
    }

    @Transactional
    public LoanAccount staffForeclose(String accountId, String paymentId, BigDecimal amount, String actorId) {
        LoanAccount account = getAccountForStaffAction(accountId);
        if (returnExistingPayment(account, paymentId, "FORECLOSURE", amount)) return account;
        ensureOpen(account);
        if (amount.compareTo(account.getOutstandingPrincipal()) != 0) {
            throw new IllegalArgumentException("Foreclosure amount must match the current quote");
        }
        collectPaymentFromBankAccount(account, paymentId, amount);
        account.setOutstandingPrincipal(ZERO.setScale(MONEY_SCALE));
        recordPayment(account, paymentId, amount, "FORECLOSURE");
        closeIfPaid(account);
        LoanApplication application = getApplication(account.getApplicationId());
        recordHistory(application, "STAFF_FORECLOSURE", application.getStatus(), application.getStatus(),
                "Staff recorded foreclosure of " + amount, actorId);
        return accountRepository.save(account);
    }

    private LoanAccount getAccountForStaffAction(String id) {
        LoanAccount account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + id));
        if (account.getStatus() != null && !account.getStatus().isBlank()
                && !"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Loan repayments are available after successful disbursement");
        }
        return account;
    }
}
