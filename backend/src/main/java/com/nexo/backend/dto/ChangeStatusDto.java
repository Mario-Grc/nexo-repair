package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusDto(
        @NotNull(message = "New status is required")
        TicketStatus newStatus,
        @Size(max = 255, message = "Note must be at most 255 characters")
        String note) {}
