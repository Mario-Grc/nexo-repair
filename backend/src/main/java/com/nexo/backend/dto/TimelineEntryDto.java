package com.nexo.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public sealed interface TimelineEntryDto permits StatusChangeEntryDto, NoteEntryDto {
    Instant occurredAt();

    // Discriminator for the frontend (@switch over entry.type).
    // @JsonProperty is required: Jackson does not serialize by default
    // methods that are not record components or JavaBean getters.
    @JsonProperty("type")
    String type();
}
