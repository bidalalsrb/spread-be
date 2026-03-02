package com.example.inputbe.controller;

import com.example.inputbe.dto.ApiResponse;
import com.example.inputbe.dto.SettingsResponse;
import com.example.inputbe.dto.UpdateSettingsRequest;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.service.SettingsService;
import com.example.inputbe.service.SheetsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final SheetsService sheetsService;

    public SettingsController(SettingsService settingsService, SheetsService sheetsService) {
        this.settingsService = settingsService;
        this.sheetsService = sheetsService;
    }

    @GetMapping
    public SettingsResponse get() {
        AppSettings settings = settingsService.getSettings();
        return new SettingsResponse(true, settings.getSpreadsheetId(), settings.getSheetName());
    }

    @PutMapping
    public ResponseEntity<ApiResponse> update(@Valid @RequestBody UpdateSettingsRequest request) {
        settingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.ok("saved"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse> testConnection() {
        AppSettings settings = settingsService.getSettings();
        sheetsService.testConnection(settings);
        return ResponseEntity.ok(ApiResponse.ok("connected"));
    }
}
