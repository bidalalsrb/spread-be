package com.example.inputbe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateAbsenceBatchRequest(
        @NotEmpty List<@Valid AbsenceItemRequest> items
) {
}
