package com.example.inputbe.dto;

public enum RecordCategory {
    OBSERVATION("관찰"),
    COUNSELING("상담"),
    PLAY_LOG("놀이기록"),
    ABSENCE("결석");

    private final String sheetName;

    RecordCategory(String sheetName) {
        this.sheetName = sheetName;
    }

    public String sheetName() {
        return sheetName;
    }
}
