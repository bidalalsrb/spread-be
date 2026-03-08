package com.example.inputbe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMemoRequest(
        @NotNull RecordCategory category,
        @NotBlank @Size(max = 100) String studentName,
        @NotBlank @Size(max = 2000) String content
) {
}
