package com.nexo.backend.dto;

// Body de POST /api/tickets
public record CreateTicketDto(
        String description,
        String deviceInfo,
        Long customerId
) {}
