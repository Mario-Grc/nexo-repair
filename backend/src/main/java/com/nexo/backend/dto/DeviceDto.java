package com.nexo.backend.dto;

import com.nexo.backend.model.DeviceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceDto(
        @NotNull(message = "Device type is required")
        DeviceType type,
        @Size(max = 100, message = "Brand must be at most 100 characters")
        String brand,
        @Size(max = 100, message = "Model must be at most 100 characters")
        String model,
        @Size(max = 100, message = "Identifier must be at most 100 characters")
        String identifier) {}