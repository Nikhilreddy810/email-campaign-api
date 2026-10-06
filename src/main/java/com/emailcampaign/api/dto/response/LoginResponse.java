package com.emailcampaign.api.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresInMinutes;

    public LoginResponse(String token, long expiresInMinutes) {
        this.token = token;
        this.expiresInMinutes = expiresInMinutes;
    }
}
