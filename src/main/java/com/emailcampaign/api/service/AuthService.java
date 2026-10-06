package com.emailcampaign.api.service;

import com.emailcampaign.api.dto.request.LoginRequest;
import com.emailcampaign.api.dto.response.LoginResponse;
import com.emailcampaign.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final String adminUsername;
    private final String adminPassword;

    public AuthService(JwtUtil jwtUtil,
                        @Value("${app.admin.username}") String adminUsername,
                        @Value("${app.admin.password}") String adminPassword) {
        this.jwtUtil = jwtUtil;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public LoginResponse login(LoginRequest request) {
        // single admin account from config, no user table / signup flow
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            throw new BadCredentialsException("invalid username or password");
        }

        String token = jwtUtil.generateToken(request.getUsername());
        return new LoginResponse(token, jwtUtil.getExpirationMinutes());
    }
}
