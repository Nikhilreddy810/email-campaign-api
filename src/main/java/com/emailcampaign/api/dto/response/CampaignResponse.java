package com.emailcampaign.api.dto.response;

import com.emailcampaign.api.entity.Campaign;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class CampaignResponse {

    private Long id;
    private String name;
    private String subject;
    private String senderEmail;
    private String content;
    private OffsetDateTime scheduledAt;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getSubject(),
                campaign.getSenderEmail(),
                campaign.getContent(),
                campaign.getScheduledAt(),
                campaign.getStatus().name(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
