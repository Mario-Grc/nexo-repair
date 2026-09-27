package com.nexo.backend.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("Email already in use");
    }
}
