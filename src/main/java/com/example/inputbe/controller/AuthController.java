package com.example.inputbe.controller;

import com.example.inputbe.dto.LoginRequest;
import com.example.inputbe.dto.LoginResponse;
import com.example.inputbe.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.code());
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(LoginResponse.fail("access denied"));
        }
        return ResponseEntity.ok(LoginResponse.success(token));
    }
}
