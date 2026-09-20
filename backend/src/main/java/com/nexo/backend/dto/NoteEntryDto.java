package com.nexo.backend.dto;

import java.time.Instant;

public record NoteEntryDto(
        Instant occurredAt,
        String authorName,
        String text
) implements TimelineEntryDto {}
