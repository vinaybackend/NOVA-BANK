package com.nova.bank.transaction.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventPublisher {
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final String TRANSACTION_TOPIC="transaction-topic";
    private final Logger logger= LoggerFactory.getLogger(TransactionEventPublisher.class);

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTransactionCreatedEvent(TransactionEvent transactionEvent){
        kafkaTemplate.send(TRANSACTION_TOPIC,transactionEvent);
        logger.info("transaction created successfully : " + transactionEvent);
    }
}
