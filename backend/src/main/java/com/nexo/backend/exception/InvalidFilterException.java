package com.nexo.backend.exception;

// Invalid combination of ticket list filters. Mapped to 400.
public class InvalidFilterException extends RuntimeException {

    public InvalidFilterException(String message) {
        super(message);
    }
}
