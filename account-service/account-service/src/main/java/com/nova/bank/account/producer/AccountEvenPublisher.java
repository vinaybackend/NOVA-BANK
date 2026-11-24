package com.nova.bank.account.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccountEvenPublisher {
    Logger logger= LoggerFactory.getLogger(AccountEvenPublisher.class);
    private final String ACCOUNT_TOPIC="account-topic";
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public AccountEvenPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void accountEventCreated(AccountEvent accountEvent) {

                kafkaTemplate.send(ACCOUNT_TOPIC, accountEvent)
                        .whenComplete((result, exception) -> {

                            if (exception != null) {
                                logger.error(
                                        "Failed to publish account event to Kafka",
                                        exception
                                );
                                return;
                            }

                            logger.info(
                                    "Account event published successfully. Topic={}, Partition={}, Offset={}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset()
                            );
                        });
            }
        }


