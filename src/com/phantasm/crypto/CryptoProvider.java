package com.phantasm.crypto;

import com.phantasm.exception.DecryptionException;

import javax.crypto.SecretKey;

/**
 * Interface defining cryptographic operations for bytecode encryption and decryption.
 * Part of Core Academic Module 2: Inheritance & Interfaces.
 */
public interface CryptoProvider {

    /**
     * Encrypts plaintext bytes using a 256-bit SecretKey and 12-byte IV.
     *
     * @param plaintext raw bytecode bytes
     * @param key       secret encryption key
     * @param iv        initialization vector
     * @return ciphertext bytes including authentication tag
     * @throws DecryptionException if encryption fails
     */
    byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws DecryptionException;

    /**
     * Decrypts ciphertext bytes back to raw bytecode.
     *
     * @param ciphertext encrypted bytecode
     * @param key        secret encryption key
     * @param iv        initialization vector
     * @return decrypted raw bytecode bytes
     * @throws DecryptionException if decryption or authentication fails
     */
    byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws DecryptionException;

    /**
     * Generates a cryptographically strong SecretKey.
     */
    SecretKey generateKey() throws Exception;

    /**
     * Generates a random Initialization Vector (IV).
     */
    byte[] generateIv();

    /**
     * Converts a 64-character Hex string to a SecretKey.
     */
    SecretKey keyFromHex(String hexKey);

    /**
     * Converts a SecretKey to a 64-character Hex string.
     */
    String keyToHex(SecretKey key);
}
