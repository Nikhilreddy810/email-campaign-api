package com.emailcampaign.api.dto.response;

import com.emailcampaign.api.entity.Recipient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecipientResponse {

    private Long id;
    private String name;
    private String email;
    private String status;

    public static RecipientResponse from(Recipient recipient) {
        return new RecipientResponse(
                recipient.getId(),
                recipient.getName(),
                recipient.getEmail(),
                recipient.getStatus().name()
        );
    }
}
