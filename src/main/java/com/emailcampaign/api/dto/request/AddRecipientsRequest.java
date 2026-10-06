package com.emailcampaign.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddRecipientsRequest {

    @NotEmpty(message = "recipients must contain at least one entry")
    @Valid
    private List<RecipientItem> recipients;
}
