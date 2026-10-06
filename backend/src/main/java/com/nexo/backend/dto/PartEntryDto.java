package com.nexo.backend.dto;

import java.time.Instant;

// type is a record component so it implements TimelineEntryDto.type
// and Jackson serializes it without annotations.
public record PartEntryDto(
        Instant occurredAt,
        String authorName,
        String type,
        String description,
        int quantity) implements TimelineEntryDto {
}
