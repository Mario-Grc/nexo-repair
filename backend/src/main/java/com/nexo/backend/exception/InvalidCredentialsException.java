package com.nexo.backend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Incorrect email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
