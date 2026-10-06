package com.nexo.backend.dto;

import java.time.Instant;

// type is a record component so Jackson serializes it without annotations.
// The short constructor keeps the usual call sites readable.
public record AssignmentChangeEntryDto(
        Instant occurredAt,
        String authorName,
        String previousEmployeeName,
        String newEmployeeName,
        String type
) implements TimelineEntryDto {
    public AssignmentChangeEntryDto(Instant occurredAt, String authorName,
                                    String previousEmployeeName, String newEmployeeName) {
        this(occurredAt, authorName, previousEmployeeName, newEmployeeName, "ASSIGNMENT_CHANGE");
    }
}
