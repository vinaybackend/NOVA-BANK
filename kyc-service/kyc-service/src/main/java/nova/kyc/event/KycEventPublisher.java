package nova.kyc.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KycEventPublisher {
    private final Logger logger=LoggerFactory.getLogger(KycEventPublisher.class);
    private final String KYC_TOPIC="kyc-topic";
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public KycEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void kycEventPublish(KycEvent kycEvent){
        kafkaTemplate.send(KYC_TOPIC,kycEvent);
        logger.info("kyc event publish successfully : " + kycEvent);
    }
}
