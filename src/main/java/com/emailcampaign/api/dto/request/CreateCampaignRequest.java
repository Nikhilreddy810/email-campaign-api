package com.emailcampaign.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateCampaignRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "senderEmail is required")
    @Email(message = "senderEmail must be a valid email address")
    private String senderEmail;

    @NotBlank(message = "content is required")
    private String content;

    @NotNull(message = "scheduledAt is required")
    @Future(message = "scheduledAt must be in the future")
    private OffsetDateTime scheduledAt;
}
