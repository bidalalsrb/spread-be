package com.example.inputbe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @NotBlank @Size(max = 255) String spreadsheetId,
        @Size(max = 100) String sheetName
) {
}
