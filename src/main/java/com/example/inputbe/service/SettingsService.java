package com.example.inputbe.service;

import com.example.inputbe.dto.UpdateSettingsRequest;
import com.example.inputbe.entity.AppSettings;
import com.example.inputbe.repository.AppSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final Long SETTINGS_ID = 1L;
    private static final String LEGACY_SHEET_NAME_MEMO = "메모";
    private static final String LEGACY_SHEET_NAME_MEMO_LOG = "메모기록";
    private static final String DEFAULT_SHEET_NAME = "관찰";

    private final AppSettingsRepository appSettingsRepository;

    @Value("${app.sheets.fixed-spreadsheet:}")
    private String fixedSpreadsheet;

    public SettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public AppSettings getSettings() {
        AppSettings settings = appSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::createDefault);
        migrateLegacySheetName(settings);
        applyFixedSpreadsheet(settings);
        return settings;
    }

    public AppSettings updateSettings(UpdateSettingsRequest request) {
        AppSettings settings = getSettings();
        settings.setSheetName(normalizeSheetName(request.sheetName()));
        applyFixedSpreadsheet(settings);
        return appSettingsRepository.save(settings);
    }

    private AppSettings createDefault() {
        AppSettings settings = new AppSettings();
        settings.setId(SETTINGS_ID);
        settings.setSpreadsheetId("");
        settings.setSheetName(DEFAULT_SHEET_NAME);
        return appSettingsRepository.save(settings);
    }

    private void applyFixedSpreadsheet(AppSettings settings) {
        if (fixedSpreadsheet != null && !fixedSpreadsheet.isBlank()) {
            settings.setSpreadsheetId(fixedSpreadsheet.trim());
        }
    }

    private void migrateLegacySheetName(AppSettings settings) {
        String sheetName = settings.getSheetName();
        if (LEGACY_SHEET_NAME_MEMO.equals(sheetName) || LEGACY_SHEET_NAME_MEMO_LOG.equals(sheetName)) {
            settings.setSheetName(DEFAULT_SHEET_NAME);
            appSettingsRepository.save(settings);
        }
    }

    private String normalizeSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return DEFAULT_SHEET_NAME;
        }
        return sheetName.trim();
    }

}
