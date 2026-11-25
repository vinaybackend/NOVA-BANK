package com.nova.bank.loan.controller;

import com.nova.bank.loan.dto.*;
import com.nova.bank.loan.entity.*;
import com.nova.bank.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/loan-products")
    public List<LoanProduct> getLoanProducts() {
        return loanService.getAvailableProducts();
    }

    @PostMapping("/loan-products")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER','LOAN_OFFICER')")
    public LoanProduct createLoanProduct(@Valid @RequestBody CreateLoanProductRequest request) {
        return loanService.createProduct(request);
    }

    @PostMapping("/loan-products/{productId}/estimate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanEstimateResponse estimate(@PathVariable Long productId, @Valid @RequestBody LoanEstimateRequest request) {
        return loanService.estimate(productId, request);
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public LoanApplication apply(@Valid @RequestBody ApplyForLoanRequest request) {
        return loanService.apply(request);
    }

    @PostMapping("/applications/{applicationId}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanApplication submitApplication(@PathVariable String applicationId) {
        return loanService.submitApplication(applicationId);
    }

    @GetMapping("/applications/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public List<LoanApplication> getMyApplications() {

        return loanService.getMyApplications();
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public List<LoanApplication> getApplicationsForStaff() {

        return loanService.getApplicationsForStaff();
    }

    @GetMapping("/applications/assigned")
    @PreAuthorize("hasRole('LOAN_OFFICER', 'BRANCH_MANAGER')")
    public List<LoanApplication> getAssignedApplications(Authentication authentication) {
        return loanService.getAssignedApplications(authentication.getName());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public Map<String, Long> getDashboardStatistics() {

        return loanService.getDashboardStatistics();
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public LoanApplication getApplication(@PathVariable String applicationId, Authentication authentication) {
        if (hasRole(authentication, "CUSTOMER")) {
            return loanService.getMyApplicationDetails(applicationId);
        }
        return loanService.getApplicationForReview(applicationId);
    }

    @GetMapping("/applications/{applicationId}/review-details")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public LoanApplicationReviewResponse getApplicationReviewDetails(@PathVariable String applicationId) {
        return loanService.getApplicationReviewDetails(applicationId);
    }

    @GetMapping("/applications/{applicationId}/history")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public List<LoanApplicationHistory> getApplicationHistory(@PathVariable String applicationId) {
        return loanService.getApplicationHistory(applicationId);
    }

    @PostMapping("/applications/{applicationId}/employment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanApplication submitEmployment(@PathVariable String applicationId, @Valid @RequestBody EmploymentInfoRequest request) {
        return loanService.submitEmploymentInfo(applicationId, request);
    }

    @PostMapping(value = "/applications/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanDocumentResponse uploadDocument(@PathVariable String applicationId, @RequestParam String documentType, @RequestPart("file") MultipartFile file) {
        return loanService.uploadDocument(applicationId, documentType, file);
    }

    @GetMapping("/applications/{applicationId}/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public List<LoanDocumentResponse> getDocuments(@PathVariable String applicationId, Authentication authentication) {
        if (hasRole(authentication, "CUSTOMER")) {
            loanService.getMyApplicationDetails(applicationId);
        }
        return loanService.getDocuments(applicationId);
    }

    @GetMapping("/documents/{documentId}/file")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public ResponseEntity<Resource> getDocument(@PathVariable String documentId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(loanService.getDocumentFile(documentId));
    }

    @GetMapping("/documents/{documentId}/preview")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public ResponseEntity<Resource> previewDocument(@PathVariable String documentId,
                                                    Authentication authentication) {
        Resource file = hasRole(authentication, "CUSTOMER")
                ? loanService.getMyDocumentFile(documentId) : loanService.getDocumentFile(documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(loanService.getDocumentContentType(documentId)))
                .body(file);
    }

    @PatchMapping("/documents/{documentId}/verification")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public LoanDocumentResponse verifyDocument(@PathVariable String documentId,
                                               @RequestBody VerifyDocumentRequest request,
                                               Authentication authentication) {
        return loanService.verifyDocument(documentId, request.verified(), authentication.getName());
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','KYC_OFFICER','BRANCH_MANAGER')")
    public ResponseEntity<Void> deleteRejectedDocument(@PathVariable String documentId) {
        loanService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/applications/{applicationId}/credit-assessment")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanApplication assessCredit(@PathVariable String applicationId,
                                        @Valid @RequestBody CreditAssessmentRequest request) {
        return loanService.assessCredit(applicationId, request);
    }

    @PostMapping("/applications/{applicationId}/recommendation")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanApplication recommend(@PathVariable String applicationId,
                                     @Valid @RequestBody LoanRecommendationRequest request,
                                     Authentication authentication) {
        return loanService.recommend(applicationId, request, authentication.getName());
    }

    @PostMapping("/applications/{applicationId}/final-approval")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public LoanApplication approveOffer(@PathVariable String applicationId) {
        return loanService.approveAndCreateOffer(applicationId);
    }

    @PostMapping("/applications/{applicationId}/manager-decision")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public LoanApplication decideAsManager(@PathVariable String applicationId,
                                           @Valid @RequestBody BranchManagerDecisionRequest request,
                                           Authentication authentication) {
        return loanService.decideAsManager(applicationId, request, authentication.getName());
    }

    @PostMapping("/applications/{applicationId}/assignment")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public LoanApplication assignApplication(@PathVariable String applicationId,
                                             @Valid @RequestBody LoanAssignmentRequest request,
                                             Authentication authentication) {
        return loanService.assignApplication(applicationId, request, authentication.getName());
    }

    @PostMapping("/applications/{applicationId}/additional-documents")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanApplication requestAdditionalDocuments(@PathVariable String applicationId,
                                                      @Valid @RequestBody AdditionalDocumentsRequest request,
                                                      Authentication authentication) {
        return loanService.requestAdditionalDocuments(applicationId, request, authentication.getName());
    }

    @PostMapping("/applications/{applicationId}/offer-response")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanApplication respondToOffer(@PathVariable String applicationId,
                                          @RequestBody OfferResponseRequest request) {
        return loanService.respondToOffer(applicationId, request.accepted());
    }

    @PostMapping("/applications/{applicationId}/accept-agreement")
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoanApplication acceptAgreement(@PathVariable String applicationId) {
        return loanService.acceptAgreement(applicationId);
    }

    @PostMapping("/applications/{applicationId}/prepare-disbursement")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount prepareDisbursement(@PathVariable String applicationId) {
        return loanService.prepareDisbursement(applicationId);
    }

    @GetMapping("/applications/{applicationId}/loan-account")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount getApplicationLoanAccount(@PathVariable String applicationId) {
        return loanService.getLoanAccountForApplication(applicationId);
    }

    @PostMapping("/applications/{applicationId}/disburse")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount disburse(@PathVariable String applicationId) {
        return loanService.disburse(applicationId);
    }

    @GetMapping("/accounts/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<LoanAccount> getMyAccounts() {
        return loanService.getMyAccounts();
    }

    @GetMapping("/accounts/{accountId}/schedule")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<RepaymentSchedule> getSchedule(@PathVariable String accountId) {
        return loanService.getMySchedule(accountId);
    }

    @GetMapping("/accounts/{accountId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<LoanPayment> getPayments(@PathVariable String accountId) {
        return loanService.getMyPayments(accountId);
    }

    @PostMapping("/accounts/{accountId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount payEmi(@PathVariable String accountId, @Valid @RequestBody PaymentRequest request) {
        return loanService.payEmi(accountId, request.paymentId(), request.amount());
    }

    @PostMapping("/accounts/{accountId}/prepayments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount prepay(@PathVariable String accountId, @Valid @RequestBody PaymentRequest request) {
        return loanService.prepay(accountId, request.paymentId(), request.amount());
    }

    @GetMapping("/accounts/{accountId}/foreclosure-quote")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public ForeclosureQuote getForeclosureQuote(@PathVariable String accountId) {
        return loanService.getForeclosureQuote(accountId);
    }

    @PostMapping("/accounts/{accountId}/foreclosure")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount foreclose(@PathVariable String accountId, @Valid @RequestBody PaymentRequest request) {
        return loanService.foreclose(accountId, request.paymentId(), request.amount());
    }

    @GetMapping("/staff/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<LoanAccount> getAllLoanAccountsForStaff() {
        return loanService.getAllLoanAccountsForStaff();
    }

    @GetMapping("/staff/portfolio")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<LoanPortfolioResponse> getLoanPortfolioForStaff() {

        return loanService.getLoanPortfolioForStaff();
    }

    @GetMapping("/staff/accounts/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount getLoanAccountForStaff(@PathVariable String accountId) {
        return loanService.getLoanAccountForStaff(accountId);
    }

    @GetMapping("/staff/accounts/{accountId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<RepaymentSchedule> getScheduleForStaff(@PathVariable String accountId) {
        return loanService.getScheduleForStaff(accountId);
    }

    @GetMapping("/staff/accounts/{accountId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public List<LoanPayment> getPaymentsForStaff(@PathVariable String accountId) {
        return loanService.getPaymentsForStaff(accountId);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)
                        || authority.getAuthority().equals(role));
    }

    //new
    @PostMapping("/staff/accounts/{accountId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount staffPayEmi(@PathVariable String accountId,
                                   @Valid @RequestBody PaymentRequest request,
                                   Authentication authentication) {
        return loanService.staffPayEmi(accountId, request.paymentId(), request.amount(),
                authentication.getName());
    }

    @PostMapping("/staff/accounts/{accountId}/prepayments")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount staffPrepay(@PathVariable String accountId,
                                   @Valid @RequestBody PaymentRequest request,
                                   Authentication authentication) {
        return loanService.staffPrepay(accountId, request.paymentId(), request.amount(),
                authentication.getName());
    }

    @PostMapping("/staff/accounts/{accountId}/foreclosure")
    @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER','BRANCH_MANAGER')")
    public LoanAccount staffForeclose(@PathVariable String accountId,
                                      @Valid @RequestBody PaymentRequest request,
                                      Authentication authentication) {
        return loanService.staffForeclose(accountId, request.paymentId(), request.amount(),
                authentication.getName());
    }

}
