package com.emailcampaign.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProcessResultResponse {
    private int campaignsProcessed;
    private String message;
}
