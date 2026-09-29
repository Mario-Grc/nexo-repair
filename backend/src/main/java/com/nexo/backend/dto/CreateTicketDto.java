package com.nexo.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketDto(
        @NotBlank(message = "Problem description is required")
        @Size(max = 500, message = "Problem description must be at most 500 characters")
        String problemDescription,
        @NotNull(message = "Device is required")
        @Valid
        DeviceDto device,
        @NotNull(message = "Customer is required")
        Long customerId
) {}
