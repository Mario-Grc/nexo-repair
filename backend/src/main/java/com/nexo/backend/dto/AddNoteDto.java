package com.nexo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteDto(
        @NotBlank(message = "Text is required")
        @Size(max = 500, message = "Text must be at most 500 characters")
        String text) {}
