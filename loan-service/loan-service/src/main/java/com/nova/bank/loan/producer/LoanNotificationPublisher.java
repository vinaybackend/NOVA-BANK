package com.nova.bank.loan.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoanNotificationPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LoanNotificationPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(LoanNotificationEvent event) {
        kafkaTemplate.send("loan-topic", event);
    }
}

