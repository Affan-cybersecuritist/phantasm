package com.phantasm.exception;

/**
 * Custom checked exception thrown when the AES decryption secret key is missing from environment/config.
 * Part of Core Academic Module 3: Exception Handling and I/O.
 */
public class KeyNotFoundException extends Exception {
    public KeyNotFoundException(String message) {
        super(message);
    }

    public KeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
