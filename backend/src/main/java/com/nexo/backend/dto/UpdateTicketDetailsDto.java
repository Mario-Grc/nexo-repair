package com.nexo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketDetailsDto(
        @NotBlank(message = "Problem description is required")
        @Size(max = 500, message = "Problem description must be at most 500 characters")
        String problemDescription) {
}
