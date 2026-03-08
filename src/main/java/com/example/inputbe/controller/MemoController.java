package com.example.inputbe.controller;

import com.example.inputbe.dto.CreateMemoRequest;
import com.example.inputbe.dto.MemoResponse;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.service.SettingsService;
import com.example.inputbe.service.SheetsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memos")
public class MemoController {

    private final SettingsService settingsService;
    private final SheetsService sheetsService;

    public MemoController(SettingsService settingsService, SheetsService sheetsService) {
        this.settingsService = settingsService;
        this.sheetsService = sheetsService;
    }

    @PostMapping
    public ResponseEntity<MemoResponse> create(@Valid @RequestBody CreateMemoRequest request) {
        AppSettings settings = settingsService.getSettings();
        String savedAt = sheetsService.appendMemo(
                settings,
                request.category(),
                request.studentName().trim(),
                request.content().trim()
        );
        return ResponseEntity.ok(MemoResponse.success(savedAt));
    }
}
