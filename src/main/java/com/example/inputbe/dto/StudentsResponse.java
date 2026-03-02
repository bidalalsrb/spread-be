package com.example.inputbe.dto;

import java.util.List;

public record StudentsResponse(boolean ok, List<StudentItem> items) {
}
