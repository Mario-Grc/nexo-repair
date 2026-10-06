package com.nexo.backend.dto;

import com.nexo.backend.model.TicketStatus;

import java.time.Instant;

// type is a record component so Jackson serializes it without annotations.
// The short constructor keeps the usual call sites readable.
public record StatusChangeEntryDto(
        Instant occurredAt,
        String authorName,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        String note,
        String type
) implements TimelineEntryDto {
    public StatusChangeEntryDto(Instant occurredAt, String authorName,
                                TicketStatus previousStatus, TicketStatus newStatus, String note) {
        this(occurredAt, authorName, previousStatus, newStatus, note, "STATUS_CHANGE");
    }
}
