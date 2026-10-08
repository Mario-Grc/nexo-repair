package com.nexo.backend.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.nexo.backend.model.TicketStatus;

// Query filters for GET /api/tickets. All fields are optional.
public record TicketFilter(
        List<TicketStatus> status,
        Long assignedEmployeeId,
        Boolean unassigned,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
        String q,
        Long customerId) {
}
