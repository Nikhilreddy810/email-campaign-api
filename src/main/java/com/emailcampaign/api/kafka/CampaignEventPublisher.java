package com.emailcampaign.api.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CampaignEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CampaignEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public CampaignEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${app.kafka.topic:campaign-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publishCampaignProcessed(CampaignProcessedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, String.valueOf(event.campaignId()), payload)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Kafka publish failed for campaign {}", event.campaignId(), error);
                        } else {
                            log.info("Kafka event published: topic={}, campaignId={}, offset={}",
                                    topic, event.campaignId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize campaign event", e);
        }
    }
}
