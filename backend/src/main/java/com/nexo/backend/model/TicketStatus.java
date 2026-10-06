package com.nexo.backend.model;

public enum TicketStatus {
    PENDING,
    IN_PROGRESS,
    WAITING_FOR_PARTS,
    COMPLETED,
    DELIVERED,
    CANCELLED;

    // Single source of truth. Closed tickets accept no parts changes.
    public boolean isClosed() {
        return this == DELIVERED || this == CANCELLED;
    }
}
