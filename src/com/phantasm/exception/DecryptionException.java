package com.phantasm.exception;

/**
 * Custom checked exception thrown when cryptographic decryption or authentication tag check fails.
 * Part of Core Academic Module 3: Exception Handling and I/O.
 */
public class DecryptionException extends Exception {
    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
