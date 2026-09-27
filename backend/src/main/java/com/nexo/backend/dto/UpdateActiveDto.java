package com.nexo.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveDto(
        @NotNull(message = "Active flag is required")
        Boolean active) {
}
