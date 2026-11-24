package com.nova.notification.consumer;

import com.nova.notification.services.AccountEmailService;
import com.nova.notification.services.KycEmailService;
import com.nova.notification.services.LoanEmailService;
import com.nova.notification.services.TransactionEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventConsumer {
    private final TransactionEmailService transactionEmailService;
    private final KycEmailService kycEmailService;
    Logger logger= LoggerFactory.getLogger(TransactionEventConsumer.class);
    private final String TRANSACTION_TOPIC = "transaction-topic";
    private final String ACCOUNT_TOPIC="account-topic";
    private final String KYC_TOPIC="kyc-topic";

    private final AccountEmailService accountEmailService;
    private final LoanEmailService loanEmailService;

    public TransactionEventConsumer(TransactionEmailService transactionEmailService, KycEmailService kycEmailService, AccountEmailService accountEmailService, LoanEmailService loanEmailService) {
        this.transactionEmailService = transactionEmailService;
        this.kycEmailService = kycEmailService;
        this.accountEmailService = accountEmailService;
        this.loanEmailService = loanEmailService;
    }

    @KafkaListener(topics = TRANSACTION_TOPIC)
    public void consumeTransactionEvent(TransactionEvent transactionEvent){
       logger.info("Receive transaction event : eventId={},customerId={},transactionStatus={}",
               transactionEvent.getEventId(),transactionEvent.getCustomerId(),transactionEvent.getTransactionStatus());

       if (!"SUCCESS".equals(transactionEvent.getTransactionStatus())){
           logger.info("Skipping transaction email because status is not SUCCESS. transactionId={}", transactionEvent.getTransactionId());
           return;
       }

       transactionEmailService.sendTransactionSuccessEmail(transactionEvent);
    }


    @KafkaListener(topics = ACCOUNT_TOPIC)
    public void consumeAccountEvent(AccountEvent accountEvent) {

        logger.info("Received account event: eventId={}, accountNumber={}", accountEvent.getEventId(), accountEvent.getAccountNumber());

        accountEmailService.sendAccountOpenedEmail(accountEvent);
    }

    @KafkaListener(topics =KYC_TOPIC)
    public void consumeKycEvent(KycEvent kycEvent){
        logger.info("kyc event consume success fully : name={},email={}",kycEvent.getName(),kycEvent.getEmail());
        kycEmailService.sendAccountOpeningRequestEmail(kycEvent);
    }

    @KafkaListener(topics = "loan-topic")
    public void consumeLoanEvent(LoanEvent loanEvent) {
        loanEmailService.sendLoanEmail(loanEvent);
    }
}