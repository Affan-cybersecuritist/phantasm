package com.phantasm.exception;

/**
 * Custom checked exception thrown when integrity check (SHA-256) or bytecode validation fails.
 * Part of Core Academic Module 3: Exception Handling and I/O.
 */
public class ClassSecurityException extends Exception {
    public ClassSecurityException(String message) {
        super(message);
    }

    public ClassSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
