package com.example.inputbe.dto;

public record LoginResponse(boolean ok, String token, String message) {
    public static LoginResponse success(String token) {
        return new LoginResponse(true, token, null);
    }

    public static LoginResponse fail(String message) {
        return new LoginResponse(false, null, message);
    }
}
