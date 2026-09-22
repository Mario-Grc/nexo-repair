package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketDto(
        UUID publicId,
        String problemDescription,
        DeviceDto device,
        TicketStatus status,
        Long customerId,
        String customerName,
        Long assignedEmployeeId,
        String assignedEmployeeName,
        Instant createdAt
) {}