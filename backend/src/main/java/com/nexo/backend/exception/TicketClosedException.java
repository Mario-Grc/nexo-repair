package com.nexo.backend.exception;

public class TicketClosedException extends RuntimeException {
    public TicketClosedException() {
        super("Ticket is closed");
    }
}
