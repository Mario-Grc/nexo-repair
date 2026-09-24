package com.nexo.backend.dto;

import java.time.Instant;

public record AssignmentChangeEntryDto(
        Instant occurredAt,
        String authorName,
        String previousEmployeeName,
        String newEmployeeName
) implements TimelineEntryDto {
    @Override
    public String type() {
        return "ASSIGNMENT_CHANGE";
    }
}
