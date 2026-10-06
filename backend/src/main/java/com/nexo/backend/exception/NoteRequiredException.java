package com.nexo.backend.exception;

import com.nexo.backend.model.TicketStatus;

public class NoteRequiredException extends RuntimeException {
    public NoteRequiredException(TicketStatus status) {
        super(messageFor(status));
    }

    private static String messageFor(TicketStatus status) {
        if (status == TicketStatus.CANCELLED) {
            return "A cancellation reason is required to cancel the ticket";
        }
        if (status == TicketStatus.COMPLETED) {
            return "A repair summary is required to complete the ticket";
        }
        return "A note is required for this status";
    }
}
