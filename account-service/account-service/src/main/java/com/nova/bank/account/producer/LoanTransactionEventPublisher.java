package com.nova.bank.account.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoanTransactionEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoanTransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(LoanTransactionEvent event) {
        kafkaTemplate.send("transaction-topic", event);
    }
}

