package com.emailcampaign.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CampaignStatisticsResponse {

    private CampaignResponse campaign;
    private long totalRecipients;
    private long delivered;
    private long failed;
    private long pending;
}
