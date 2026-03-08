package com.example.inputbe.controller;

import com.example.inputbe.dto.ApiResponse;
import com.example.inputbe.dto.AbsenceTodayResponse;
import com.example.inputbe.dto.CreateAbsenceBatchRequest;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.service.SettingsService;
import com.example.inputbe.service.SheetsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private final SettingsService settingsService;
    private final SheetsService sheetsService;

    public AbsenceController(SettingsService settingsService, SheetsService sheetsService) {
        this.settingsService = settingsService;
        this.sheetsService = sheetsService;
    }

    @GetMapping("/today")
    public AbsenceTodayResponse today() {
        AppSettings settings = settingsService.getSettings();
        return new AbsenceTodayResponse(true, sheetsService.todayAbsenceDate(), sheetsService.getTodayAbsences(settings));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse> submit(@Valid @RequestBody CreateAbsenceBatchRequest request) {
        AppSettings settings = settingsService.getSettings();
        sheetsService.appendAbsenceBatch(settings, request.items());
        return ResponseEntity.ok(ApiResponse.ok("submitted"));
    }
}
