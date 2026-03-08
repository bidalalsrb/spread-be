package com.example.inputbe.dto;

import java.util.List;

public record AbsenceTodayResponse(boolean ok, String date, List<AbsenceItemResponse> items) {
}
