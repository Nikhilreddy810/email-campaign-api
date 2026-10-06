package com.emailcampaign.api.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CampaignEventListener {

    private static final Logger log = LoggerFactory.getLogger(CampaignEventListener.class);

    @KafkaListener(topics = "${app.kafka.topic:campaign-events}", groupId = "email-campaign-ops")
    public void consume(String event) {
        log.info("Kafka event consumed by ops listener: {}", event);
    }
}
