package com.example.inputbe.dto;

public record MemoResponse(boolean ok, String savedAt, String message) {
    public static MemoResponse success(String savedAt) {
        return new MemoResponse(true, savedAt, null);
    }

    public static MemoResponse fail(String message) {
        return new MemoResponse(false, null, message);
    }
}
