package com.nexo.backend.dto;

import java.time.Instant;

public sealed interface TimelineEntryDto permits StatusChangeEntryDto, NoteEntryDto {
    Instant occurredAt();
}
