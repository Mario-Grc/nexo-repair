package com.nexo.backend.dto;

import java.time.Instant;

// type is a record component so Jackson serializes it without annotations.
// The short constructor keeps the usual call sites readable.
public record NoteEntryDto(
        Instant occurredAt,
        String authorName,
        String text,
        String type
) implements TimelineEntryDto {
    public NoteEntryDto(Instant occurredAt, String authorName, String text) {
        this(occurredAt, authorName, text, "NOTE");
    }
}
