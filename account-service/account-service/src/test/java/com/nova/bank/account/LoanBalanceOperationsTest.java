package com.nova.bank.account;

import com.nova.bank.account.clients.AuthClient;
import com.nova.bank.account.clients.BranchClient;
import com.nova.bank.account.clients.CustomerClient;
import com.nova.bank.account.dto.LoanBalanceRequest;
import com.nova.bank.account.dto.CustomerProfileResponse;
import com.nova.bank.account.entities.Account;
import com.nova.bank.account.entities.AccountStatus;
import com.nova.bank.account.entities.AccountType;
import com.nova.bank.account.entities.LoanBalanceOperation;
import com.nova.bank.account.producer.AccountEvenPublisher;
import com.nova.bank.account.repositories.AccountRepository;
import com.nova.bank.account.repositories.LoanBalanceOperationRepository;
import com.nova.bank.account.services.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanBalanceOperationsTest {

    private AccountRepository accountRepository;
    private LoanBalanceOperationRepository operationRepository;
    private CustomerClient customerClient;
    private AccountService accountService;
    private Account customerAccount;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        operationRepository = mock(LoanBalanceOperationRepository.class);
        customerClient = mock(CustomerClient.class);
        accountService = new AccountService(
                customerClient,
                accountRepository,
                mock(BranchClient.class),
                mock(AccountEvenPublisher.class),
                mock(AuthClient.class),
                operationRepository);

        customerAccount = new Account();
        customerAccount.setAccountNumber("1234567890");
        customerAccount.setCustomerId("CUST-1");
        customerAccount.setAccountType(AccountType.SAVINGS);
        customerAccount.setAccountStatus(AccountStatus.ACTIVE);
        customerAccount.setBalance(new BigDecimal("1000.00"));
        customerAccount.setCurrency("INR");

        when(operationRepository.findById("LOAN-DISBURSE-1")).thenReturn(Optional.empty());
        when(accountRepository.findActiveAccountsForUpdate(
                "CUST-1", AccountType.SAVINGS, AccountStatus.ACTIVE))
                .thenReturn(List.of(customerAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creditsDisbursementToTheCustomersActiveAccount() {
        var response = accountService.creditLoanDisbursement(new LoanBalanceRequest(
                "LOAN-DISBURSE-1", "CUST-1", null, new BigDecimal("500.00")));

        assertEquals(new BigDecimal("1500.00"), response.balance());
        assertEquals("1234567890", response.accountNumber());
        verify(operationRepository).save(any(LoanBalanceOperation.class));
    }

    @Test
    void retryingTheSameDisbursementDoesNotCreditTwice() {
        accountService.creditLoanDisbursement(new LoanBalanceRequest(
                "LOAN-DISBURSE-1", "CUST-1", null, new BigDecimal("500.00")));

        LoanBalanceOperation savedOperation = new LoanBalanceOperation();
        savedOperation.setOperationId("LOAN-DISBURSE-1");
        savedOperation.setCustomerId("CUST-1");
        savedOperation.setAccountNumber("1234567890");
        savedOperation.setOperationType("DISBURSEMENT");
        savedOperation.setAmount(new BigDecimal("500.00"));
        when(operationRepository.findById("LOAN-DISBURSE-1"))
                .thenReturn(Optional.of(savedOperation));
        when(accountRepository.findByAccountNumberForUpdate("1234567890"))
                .thenReturn(Optional.of(customerAccount));

        var response = accountService.creditLoanDisbursement(new LoanBalanceRequest(
                "LOAN-DISBURSE-1", "CUST-1", null, new BigDecimal("500.00")));

        assertEquals(new BigDecimal("1500.00"), response.balance());
        verify(operationRepository, times(1)).save(any(LoanBalanceOperation.class));
    }

    @Test
    void loanPaymentDebitsTheCustomerAccountOnlyOnce() {
        CustomerProfileResponse profile = new CustomerProfileResponse();
        ReflectionTestUtils.setField(profile, "customerId", "CUST-1");
        when(customerClient.getMyProfile()).thenReturn(profile);
        when(accountRepository.findByAccountNumberForUpdate("1234567890"))
                .thenReturn(Optional.of(customerAccount));
        when(operationRepository.findById("LOAN-PAYMENT-1"))
                .thenReturn(Optional.empty());

        LoanBalanceRequest request = new LoanBalanceRequest(
                "LOAN-PAYMENT-1", "CUST-1", "1234567890", new BigDecimal("200.00"));
        var firstResponse = accountService.debitLoanPayment(request);

        LoanBalanceOperation savedOperation = new LoanBalanceOperation();
        savedOperation.setOperationId("LOAN-PAYMENT-1");
        savedOperation.setCustomerId("CUST-1");
        savedOperation.setAccountNumber("1234567890");
        savedOperation.setOperationType("PAYMENT");
        savedOperation.setAmount(new BigDecimal("200.00"));
        when(operationRepository.findById("LOAN-PAYMENT-1"))
                .thenReturn(Optional.of(savedOperation));
        var retryResponse = accountService.debitLoanPayment(request);

        assertEquals(new BigDecimal("800.00"), firstResponse.balance());
        assertEquals(new BigDecimal("800.00"), retryResponse.balance());
        verify(operationRepository, times(1)).save(any(LoanBalanceOperation.class));
    }
}
