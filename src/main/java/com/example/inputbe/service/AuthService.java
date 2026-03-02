package com.example.inputbe.service;

import com.example.inputbe.entity.AuthToken;
import com.example.inputbe.repository.AuthTokenRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthTokenRepository authTokenRepository;

    @Value("${app.auth.code}")
    private String authCode;

    public AuthService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    public String login(String code) {
        if (!authCode.equals(code)) {
            return null;
        }

        AuthToken authToken = new AuthToken();
        authToken.setToken(UUID.randomUUID().toString());
        authTokenRepository.save(authToken);
        return authToken.getToken();
    }

    public boolean isValidToken(String token) {
        return authTokenRepository.findById(token)
                .map(t -> !t.isRevoked())
                .orElse(false);
    }
}
