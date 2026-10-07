package com.phantasm.tools;

import com.phantasm.crypto.AesGcmCryptoProvider;
import com.phantasm.crypto.CryptoProvider;
import com.phantasm.db.BytecodeRepository;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

/**
 * Build-time and runtime tool for encrypting compiled bytecode (.class) files
 * and storing them as BLOBs in SQLite database.
 * Part of Core Academic Module 5: Collections Framework & Database Connectivity (JDBC).
 */
public class Encryptor {

    private final CryptoProvider cryptoProvider;
    private final BytecodeRepository repository;

    public Encryptor(CryptoProvider cryptoProvider, BytecodeRepository repository) {
        this.cryptoProvider = cryptoProvider;
        this.repository = repository;
    }

    /**
     * Encrypts a single arbitrary .class file path and stores it directly into SQLite BLOB storage.
     * Auto-detects class name if optionalClassName is null or blank.
     *
     * @param classFilePath     Path to the .class file
     * @param optionalClassName Fully qualified class name, or null/blank for auto-detection
     * @param secretKey         AES-256 SecretKey
     * @return Fully qualified class name of encrypted payload
     */
    public String encryptFile(Path classFilePath, String optionalClassName, SecretKey secretKey) throws Exception {
        repository.initializeTables();

        if (!Files.exists(classFilePath)) {
            throw new IllegalArgumentException("Class file not found at path: " + classFilePath.toAbsolutePath());
        }

        byte[] plaintext = Files.readAllBytes(classFilePath);
        String className = (optionalClassName != null && !optionalClassName.isBlank())
                ? optionalClassName
                : ClassFileHelper.extractClassName(plaintext);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] checksum = md.digest(plaintext);
        byte[] iv = cryptoProvider.generateIv();
        byte[] ciphertext = cryptoProvider.encrypt(plaintext, secretKey, iv);

        repository.saveEncryptedClass(className, iv, plaintext.length, checksum, ciphertext);

        System.out.printf("[Encryptor] Encrypted & Stored into SQLite -> %-35s (%d bytes -> %d bytes ciphertext)%n",
                className, plaintext.length, ciphertext.length);

        return className;
    }

    /**
     * Encrypts target class files within a compiled classes directory.
     *
     * @param compiledClassesDir directory containing compiled .class files
     * @param secretKey          AES-256 SecretKey
     * @param classNames         array of fully qualified class names
     */
    public void encryptAndStore(Path compiledClassesDir, SecretKey secretKey, String... classNames) throws Exception {
        repository.initializeTables();
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        for (String className : classNames) {
            String relativePath = className.replace('.', '/') + ".class";
            Path classFile = compiledClassesDir.resolve(relativePath);

            if (!Files.exists(classFile)) {
                System.err.println("[Encryptor] SKIP (file not found): " + classFile);
                continue;
            }

            byte[] plaintext = Files.readAllBytes(classFile);
            byte[] checksum = md.digest(plaintext);
            byte[] iv = cryptoProvider.generateIv();
            byte[] ciphertext = cryptoProvider.encrypt(plaintext, secretKey, iv);

            repository.saveEncryptedClass(className, iv, plaintext.length, checksum, ciphertext);

            System.out.printf("[Encryptor] Encrypted & Stored into SQLite -> %-35s (%d bytes -> %d bytes ciphertext)%n",
                    className, plaintext.length, ciphertext.length);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: Encryptor <compiledClassesDirOrFile> <hexSecretKey|GENERATE> <class1OrAuto> [class2 ...]");
            System.exit(1);
        }

        Path path = Paths.get(args[0]);
        String keyArg = args[1];

        CryptoProvider crypto = new AesGcmCryptoProvider();
        SecretKey key = "GENERATE".equalsIgnoreCase(keyArg) ? crypto.generateKey() : crypto.keyFromHex(keyArg);

        System.out.println("[Encryptor] Secret Key (Hex): " + crypto.keyToHex(key));

        BytecodeRepository repo = new BytecodeRepository("phantasm.db");
        Encryptor encryptor = new Encryptor(crypto, repo);

        if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
            String targetClassName = args.length > 2 ? args[2] : null;
            encryptor.encryptFile(path, targetClassName, key);
        } else {
            String[] targetClasses = new String[args.length - 2];
            System.arraycopy(args, 2, targetClasses, 0, targetClasses.length);
            encryptor.encryptAndStore(path, key, targetClasses);
        }
    }
}
