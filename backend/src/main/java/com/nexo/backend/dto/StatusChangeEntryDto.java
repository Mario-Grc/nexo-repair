package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;

import java.time.Instant;

public record StatusChangeEntryDto(
        Instant occurredAt,
        String authorName,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        String note
) implements TimelineEntryDto {
    @Override
    public String type() {
        return "STATUS_CHANGE";
    }
}
