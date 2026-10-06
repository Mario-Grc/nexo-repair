package com.nexo.backend.dto;

import java.time.Instant;

// type is a record component on every entry, so Jackson serializes it directly.
public sealed interface TimelineEntryDto permits StatusChangeEntryDto, NoteEntryDto, AssignmentChangeEntryDto, PartEntryDto {
    Instant occurredAt();

    // Discriminator for the frontend (@switch over entry.type).
    String type();
}
