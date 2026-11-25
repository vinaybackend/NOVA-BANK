package com.nova.bank.loan.service;

import com.nova.bank.loan.client.CustomerClient;
import com.nova.bank.loan.client.KycClient;
import com.nova.bank.loan.client.AccountClient;
import com.nova.bank.loan.dto.LoanEstimateRequest;
import com.nova.bank.loan.dto.LoanRecommendationRequest;
import com.nova.bank.loan.dto.BranchManagerDecisionRequest;
import com.nova.bank.loan.entity.LoanAccount;
import com.nova.bank.loan.entity.LoanApplication;
import com.nova.bank.loan.entity.LoanDocument;
import com.nova.bank.loan.entity.LoanProduct;
import com.nova.bank.loan.entity.LoanStatus;
import com.nova.bank.loan.entity.RepaymentSchedule;
import com.nova.bank.loan.client.CustomerResponse;
import com.nova.bank.loan.dto.LoanBalanceResponse;
import com.nova.bank.loan.dto.CustomerBankAccountResponse;
import com.nova.bank.loan.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class LoanServiceTest {

    private LoanProductRepository productRepository;
    private LoanApplicationRepository applicationRepository;
    private LoanDocumentRepository documentRepository;
    private LoanService loanService;
    private LoanAccountRepository accountRepository;
    private RepaymentScheduleRepository scheduleRepository;
    private LoanPaymentRepository paymentRepository;
    private CustomerClient customerClient;
    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        productRepository = mock(LoanProductRepository.class);
        applicationRepository = mock(LoanApplicationRepository.class);
        documentRepository = mock(LoanDocumentRepository.class);
        accountRepository = mock(LoanAccountRepository.class);
        scheduleRepository = mock(RepaymentScheduleRepository.class);
        paymentRepository = mock(LoanPaymentRepository.class);
        customerClient = mock(CustomerClient.class);
        accountClient = mock(AccountClient.class);
        loanService = new LoanService(
                productRepository,
                applicationRepository,
                documentRepository,
                accountRepository,
                scheduleRepository,
                paymentRepository,
                mock(LoanApplicationHistoryRepository.class),
                customerClient,
                mock(KycClient.class),
                accountClient,
                "target/test-loan-documents");

        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName("Personal loan");
        product.setActive(true);
        product.setMinimumAmount(new BigDecimal("10000"));
        product.setMaximumAmount(new BigDecimal("500000"));
        product.setAnnualInterestRate(new BigDecimal("12"));
        product.setMinimumTenureMonths(6);
        product.setMaximumTenureMonths(60);
        product.setMaximumFoirPercent(new BigDecimal("40"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(accountRepository.save(any(LoanAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void calculatesEmiAndMarksAffordableApplicationEligible() {
        var estimate = loanService.estimate(1L, new LoanEstimateRequest(
                new BigDecimal("100000"), 12,
                new BigDecimal("50000"), new BigDecimal("1000")));

        assertEquals(new BigDecimal("8884.88"), estimate.monthlyEmi());
        assertEquals(new BigDecimal("19.77"), estimate.estimatedFoirPercent());
        assertTrue(estimate.eligible());
    }

    @Test
    void marksApplicationIneligibleWhenEmiExceedsFoirLimit() {
        var estimate = loanService.estimate(1L, new LoanEstimateRequest(
                new BigDecimal("100000"), 12,
                new BigDecimal("10000"), new BigDecimal("3000")));

        assertEquals(new BigDecimal("1000.00"), estimate.maximumEligibleEmi());
        assertFalse(estimate.eligible());
    }

    @Test
    void monthlyEmiPaymentDebitsCustomerBankAccountAndUpdatesLoanBalance() {
        LoanAccount account = new LoanAccount();
        account.setId("LOAN-1");
        account.setCustomerId("CUST-1");
        account.setBankAccountNumber("1234567890");
        account.setOutstandingPrincipal(new BigDecimal("10000.00"));
        account.setClosed(false);
        when(accountRepository.findByIdForUpdate("LOAN-1")).thenReturn(Optional.of(account));
        when(customerClient.getMyProfile())
                .thenReturn(new CustomerResponse("CUST-1", "Customer", "customer@example.com",
                        "5550100", "ACTIVE"));
        when(paymentRepository.findByIdAndLoanAccountId(anyString(), eq("LOAN-1")))
                .thenReturn(Optional.empty());

        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setLoanAccountId("LOAN-1");
        schedule.setInstallmentNumber(1);
        schedule.setDueDate(LocalDate.now().plusMonths(1));
        schedule.setPrincipalDue(new BigDecimal("800.00"));
        schedule.setInterestDue(new BigDecimal("100.00"));
        schedule.setAmountDue(new BigDecimal("900.00"));
        schedule.setAmountPaid(BigDecimal.ZERO);
        schedule.setStatus("PENDING");
        when(scheduleRepository.findByLoanAccountIdOrderByInstallmentNumber("LOAN-1"))
                .thenReturn(List.of(schedule));

        LoanAccount updated = loanService.payEmi("LOAN-1", "payment-1", new BigDecimal("900.00"));

        assertEquals(new BigDecimal("9200.00"), updated.getOutstandingPrincipal());
        assertEquals("PAID", schedule.getStatus());
        verify(accountClient).collectLoanPayment(any());
        verify(paymentRepository).save(any());
    }

    @Test
    void disbursementPreparesPendingAccountThenActivatesAfterBankTransfer() {
        LoanApplication application = new LoanApplication();
        application.setId("application-1");
        application.setCustomerId("CUST-1");
        application.setProductId(1L);
        application.setRequestedAmount(new BigDecimal("100000.00"));
        application.setTenureMonths(12);
        application.setStatus(LoanStatus.AGREEMENT_ACCEPTED);
        when(applicationRepository.findByIdForUpdate("application-1"))
                .thenReturn(Optional.of(application));
        when(accountRepository.findByApplicationId("application-1")).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product("application-product", 12)));
        when(accountClient.getCustomerAccounts("CUST-1"))
                .thenReturn(List.of(new CustomerBankAccountResponse(
                        "1234567890", "CUST-1", "SAVINGS", "ACTIVE", "INR")));
        when(accountClient.disburseLoan(any()))
                .thenReturn(new LoanBalanceResponse("1234567890", "CUST-1", "INR",
                        new BigDecimal("100000.00")));

        LoanAccount pendingAccount = loanService.prepareDisbursement("application-1");

        assertEquals("1234567890", pendingAccount.getBankAccountNumber());
        assertEquals("INR", pendingAccount.getCurrency());
        assertEquals(new BigDecimal("100000.00"), pendingAccount.getOutstandingPrincipal());
        assertEquals("PENDING_DISBURSEMENT", pendingAccount.getStatus());
        assertEquals(LocalDate.now(), pendingAccount.getDisbursedOn());
        assertEquals(LoanStatus.DISBURSEMENT_PENDING, application.getStatus());
        verify(accountClient, org.mockito.Mockito.never()).disburseLoan(any());
        verify(scheduleRepository).saveAll(any());

        when(accountRepository.findByApplicationId("application-1")).thenReturn(Optional.of(pendingAccount));
        LoanAccount activeAccount = loanService.disburse("application-1");

        assertEquals("ACTIVE", activeAccount.getStatus());
        assertEquals(LoanStatus.DISBURSED, application.getStatus());
        verify(accountClient).disburseLoan(any());
    }

    private LoanProduct product(String name, int maximumTenure) {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName(name);
        product.setAnnualInterestRate(new BigDecimal("12.00"));
        product.setActive(true);
        product.setMaximumTenureMonths(maximumTenure);
        return product;
    }

    @Test
    void loanOfficerRecommendationMovesPassingApplicationToManagerQueue() {
        LoanApplication application = new LoanApplication();
        application.setId("application-2");
        application.setProductId(1L);
        application.setRequestedAmount(new BigDecimal("100000.00"));
        application.setTenureMonths(12);
        application.setMonthlyIncome(new BigDecimal("50000.00"));
        application.setExistingMonthlyEmi(new BigDecimal("1000.00"));
        application.setStatus(LoanStatus.CREDIT_PASSED);
        when(applicationRepository.findById("application-2")).thenReturn(Optional.of(application));

        LoanApplication recommended = loanService.recommend("application-2",
                new LoanRecommendationRequest("APPROVE", "Income and credit checks passed",
                        new BigDecimal("90000.00"), new BigDecimal("12.00"), 12),
                "officer-1");

        assertEquals(LoanStatus.RECOMMENDED, recommended.getStatus());
        assertEquals(new BigDecimal("90000.00"), recommended.getRecommendedAmount());
        assertEquals("APPROVE", recommended.getRecommendation());
    }

    @Test
    void loanOfficerCanRecommendRejectionWithReasonWithoutOfferTerms() {
        LoanApplication application = new LoanApplication();
        application.setId("application-reject");
        application.setStatus(LoanStatus.CREDIT_PASSED);
        when(applicationRepository.findById("application-reject")).thenReturn(Optional.of(application));

        LoanApplication recommended = loanService.recommend("application-reject",
                new LoanRecommendationRequest("REJECT", "Income documents do not support repayment capacity",
                        null, null, null),
                "officer-1");

        assertEquals(LoanStatus.RECOMMENDED, recommended.getStatus());
        assertEquals("REJECT", recommended.getRecommendation());
        assertEquals("Income documents do not support repayment capacity",
                recommended.getRecommendationRemarks());
        assertEquals(null, recommended.getRecommendedAmount());
    }

    @Test
    void loanOfficerInboxIncludesNewUnassignedCustomerApplications() {
        LoanApplication submitted = new LoanApplication();
        submitted.setId("new-submission");
        when(applicationRepository.findOfficerInbox(eq("officer-1"), anyList()))
                .thenReturn(List.of(submitted));

        List<LoanApplication> inbox = loanService.getAssignedApplications("officer-1");

        assertEquals(List.of(submitted), inbox);
        verify(applicationRepository).findOfficerInbox(eq("officer-1"), anyList());
    }

    @Test
    void submittedApplicationsAllowOfficerDocumentVerification() {
        LoanApplication application = new LoanApplication();
        application.setId("submitted-app");
        application.setStatus(LoanStatus.SUBMITTED);
        LoanDocument document = new LoanDocument();
        document.setId("doc-1");
        document.setApplicationId("submitted-app");
        document.setDocumentType("AADHAAR");
        when(applicationRepository.findById("submitted-app")).thenReturn(Optional.of(application));
        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(document));
        when(documentRepository.save(any(LoanDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.findByApplicationId("submitted-app")).thenReturn(List.of(document));

        loanService.verifyDocument("doc-1", true, "officer-1");

        assertTrue(document.isVerified());
        assertEquals(LoanStatus.DOCUMENTS_VERIFIED, application.getStatus());
        verify(applicationRepository).save(application);
    }

    @Test
    void managerCannotApproveAnOfficerRejectionRecommendation() {
        LoanApplication application = new LoanApplication();
        application.setId("application-3");
        application.setStatus(LoanStatus.RECOMMENDED);
        application.setRecommendation("REJECT");
        when(applicationRepository.findById("application-3")).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () -> loanService.decideAsManager(
                "application-3", new BranchManagerDecisionRequest(
                        "APPROVE", "Reviewed", null, null, null), "manager-1"));
    }

    @Test
    void branchManagerCanAdjustRecommendedTermsWithinProductLimits() {
        LoanApplication application = new LoanApplication();
        application.setId("application-5");
        application.setProductId(1L);
        application.setRequestedAmount(new BigDecimal("100000.00"));
        application.setMonthlyIncome(new BigDecimal("50000.00"));
        application.setExistingMonthlyEmi(new BigDecimal("1000.00"));
        application.setRecommendation("APPROVE");
        application.setRecommendedAmount(new BigDecimal("90000.00"));
        application.setRecommendedInterestRate(new BigDecimal("12.00"));
        application.setRecommendedTenureMonths(12);
        application.setStatus(LoanStatus.RECOMMENDED);
        when(applicationRepository.findById("application-5")).thenReturn(Optional.of(application));

        LoanApplication approved = loanService.decideAsManager("application-5",
                new BranchManagerDecisionRequest("APPROVE", "Adjusted to customer capacity",
                        new BigDecimal("80000.00"), new BigDecimal("13.00"), 24),
                "manager-1");

        assertEquals(LoanStatus.OFFERED, approved.getStatus());
        assertEquals(new BigDecimal("80000.00"), approved.getRecommendedAmount());
        assertEquals(new BigDecimal("13.00"), approved.getRecommendedInterestRate());
        assertEquals(24, approved.getRecommendedTenureMonths());
    }

    @Test
    void creditAssessmentRequiresIdentityAndIncomeDocuments() {
        LoanApplication application = new LoanApplication();
        application.setId("application-4");
        application.setProductId(1L);
        application.setStatus(LoanStatus.DOCUMENTS_VERIFIED);
        application.setEmploymentType("SALARIED");
        when(applicationRepository.findById("application-4")).thenReturn(Optional.of(application));
        LoanDocument aadhaar = new LoanDocument();
        aadhaar.setDocumentType("AADHAAR");
        aadhaar.setVerified(true);
        when(documentRepository.findByApplicationId("application-4")).thenReturn(List.of(aadhaar));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> loanService.assessCredit(
                        "application-4", new com.nova.bank.loan.dto.CreditAssessmentRequest(720)));
        assertTrue(exception.getMessage().contains("PAN"));
    }

    @Test
    void applicationSubmissionRequiresIdentityAndOneIncomeDocument() {
        LoanApplication application = new LoanApplication();
        application.setId("application-draft");
        application.setCustomerId("CUST-1");
        application.setStatus(LoanStatus.DRAFT);
        application.setPurpose("Home renovation");
        application.setEmploymentType("SALARIED");
        application.setEmployerName("NOVA");
        application.setDesignation("Officer");
        application.setWorkExperienceYears(3);
        application.setMonthlyIncome(new BigDecimal("50000"));
        application.setExistingMonthlyEmi(BigDecimal.ZERO);
        when(applicationRepository.findById("application-draft")).thenReturn(Optional.of(application));
        when(customerClient.getMyProfile())
                .thenReturn(new CustomerResponse("CUST-1", "Customer", "customer@example.com",
                        "5550100", "ACTIVE"));
        when(documentRepository.findByApplicationId("application-draft")).thenReturn(List.of(
                loanDocument("AADHAAR"), loanDocument("PAN")));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> loanService.submitApplication("application-draft"));

        assertTrue(exception.getMessage().contains("SALARY_SLIP or BANK_STATEMENT"));
        assertEquals(LoanStatus.DRAFT, application.getStatus());
    }

    @Test
    void applicationSubmissionAcceptsSalarySlipOrBankStatement() {
        LoanApplication application = new LoanApplication();
        application.setId("application-draft");
        application.setCustomerId("CUST-1");
        application.setStatus(LoanStatus.DRAFT);
        application.setPurpose("Medical expenses");
        application.setEmploymentType("SALARIED");
        application.setEmployerName("NOVA");
        application.setDesignation("Officer");
        application.setWorkExperienceYears(3);
        application.setMonthlyIncome(new BigDecimal("50000"));
        application.setExistingMonthlyEmi(BigDecimal.ZERO);
        when(applicationRepository.findById("application-draft")).thenReturn(Optional.of(application));
        when(customerClient.getMyProfile())
                .thenReturn(new CustomerResponse("CUST-1", "Customer", "customer@example.com",
                        "5550100", "ACTIVE"));
        when(documentRepository.findByApplicationId("application-draft")).thenReturn(List.of(
                loanDocument("AADHAAR"), loanDocument("PAN"), loanDocument("BANK_STATEMENT")));
        when(applicationRepository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplication submitted = assertDoesNotThrow(
                () -> loanService.submitApplication("application-draft"));

        assertEquals(LoanStatus.DETAILS_SUBMITTED, submitted.getStatus());
    }

    private LoanDocument loanDocument(String type) {
        LoanDocument document = new LoanDocument();
        document.setDocumentType(type);
        return document;
    }
}
