package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;

public record TicketDto(
        Long id,
        String description,
        String deviceInfo,
        TicketStatus status,
        Long customerId,
        Long employeeId
) {}