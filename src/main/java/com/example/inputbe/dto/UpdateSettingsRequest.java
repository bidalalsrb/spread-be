package com.example.inputbe.dto;

import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @Size(max = 100) String sheetName
) {
}
