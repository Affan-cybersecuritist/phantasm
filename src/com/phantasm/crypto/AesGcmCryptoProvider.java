package com.phantasm.crypto;

import com.phantasm.exception.DecryptionException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Production-grade implementation of {@link CryptoProvider} using AES-256-GCM authenticated encryption.
 * Part of Core Academic Module 2: Inheritance & Interfaces.
 */
public class AesGcmCryptoProvider implements CryptoProvider {

    public static final int KEY_SIZE_BITS = 256;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final int GCM_TAG_LENGTH_BITS = 128;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws DecryptionException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new DecryptionException("AES-256-GCM encryption failed.", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] iv) throws DecryptionException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new DecryptionException("AES-256-GCM decryption or authentication tag verification failed.", e);
        }
    }

    @Override
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE_BITS, SECURE_RANDOM);
        return keyGen.generateKey();
    }

    @Override
    public byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    @Override
    public SecretKey keyFromHex(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Key string cannot be null or empty.");
        }
        String cleanHex = hexKey.trim();
        if (cleanHex.length() != 64) {
            throw new IllegalArgumentException("Hex secret key must be 64 hex characters (256 bits).");
        }
        byte[] keyBytes = hexToBytes(cleanHex);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public String keyToHex(SecretKey key) {
        return bytesToHex(key.getEncoded());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
