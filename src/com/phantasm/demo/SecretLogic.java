package com.phantasm.demo;

/**
 * Target payload class standing in for proprietary business logic.
 * Encrypted into SQLite BLOB storage at build time; decrypted and loaded purely in volatile RAM.
 */
public class SecretLogic {

    public boolean validateLicense(String key) {
        return key != null && key.startsWith("PHANTASM-") && key.length() == 17;
    }

    public double computeDiscountedPrice(double basePrice, int loyaltyYears) {
        double discount = Math.min(0.30, loyaltyYears * 0.04);
        return basePrice * (1 - discount);
    }

    public String secretMessage() {
        return "SecretLogic successfully decrypted in RAM, integrity verified, and executed reflectively!";
    }
}
