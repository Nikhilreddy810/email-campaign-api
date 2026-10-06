package com.emailcampaign.api.kafka;

public record CampaignProcessedEvent(Long campaignId, int recipientCount, String status) {
}
