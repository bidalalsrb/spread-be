package com.example.inputbe.service;

import com.example.inputbe.dto.UpdateSettingsRequest;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final Long SETTINGS_ID = 1L;
    private static final String DEFAULT_SHEET_NAME = "메모기록";

    private final AppSettingsRepository appSettingsRepository;

    public SettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public AppSettings getSettings() {
        return appSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::createDefault);
    }

    public AppSettings updateSettings(UpdateSettingsRequest request) {
        AppSettings settings = getSettings();
        settings.setSpreadsheetId(request.spreadsheetId().trim());
        settings.setSheetName(normalizeSheetName(request.sheetName()));
        return appSettingsRepository.save(settings);
    }

    private AppSettings createDefault() {
        AppSettings settings = new AppSettings();
        settings.setId(SETTINGS_ID);
        settings.setSpreadsheetId("");
        settings.setSheetName(DEFAULT_SHEET_NAME);
        return appSettingsRepository.save(settings);
    }

    private String normalizeSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return DEFAULT_SHEET_NAME;
        }
        return sheetName.trim();
    }

}
