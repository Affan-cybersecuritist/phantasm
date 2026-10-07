package com.phantasm.loader;

import com.phantasm.crypto.CryptoProvider;
import com.phantasm.db.BytecodeRepository;
import com.phantasm.exception.ClassSecurityException;
import com.phantasm.exception.DecryptionException;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom ClassLoader featuring:
 * 1. Thread-safe in-memory caching via ConcurrentHashMap.
 * 2. SQLite JDBC record fetching from BytecodeRepository.
 * 3. Decryption using CryptoProvider in RAM.
 * 4. SHA-256 checksum verification.
 * 5. Volatile heap scrubbing via Arrays.fill(decryptedBytes, (byte)0).
 *
 * Demonstrates Core Academic Modules 2 (Inheritance & Interfaces) & 5 (Collections & JDBC).
 */
public class PhantasmClassLoader extends ClassLoader {

    private final CryptoProvider cryptoProvider;
    private final SecretKey secretKey;
    private final BytecodeRepository repository;
    private final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    public PhantasmClassLoader(CryptoProvider cryptoProvider, SecretKey secretKey, BytecodeRepository repository, ClassLoader parent) {
        super(parent);
        this.cryptoProvider = cryptoProvider;
        this.secretKey = secretKey;
        this.repository = repository;
    }

    public PhantasmClassLoader(CryptoProvider cryptoProvider, SecretKey secretKey, BytecodeRepository repository) {
        this(cryptoProvider, secretKey, repository, PhantasmClassLoader.class.getClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Check in-memory cache first (Module 5: ConcurrentHashMap)
        Class<?> cachedClass = classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }

        byte[] decryptedBytes = null;
        try {
            // Fetch BLOB record from SQLite database
            BytecodeRepository.EncryptedClassRecord record = repository.fetchEncryptedClass(name);
            if (record == null) {
                throw new ClassNotFoundException("No encrypted BLOB record found in database for " + name);
            }

            // Decrypt bytecode strictly in volatile RAM
            decryptedBytes = cryptoProvider.decrypt(record.ciphertext(), secretKey, record.iv());

            if (decryptedBytes.length != record.payloadLength()) {
                throw new ClassSecurityException("Payload length mismatch after decryption for class " + name);
            }

            // Verify SHA-256 integrity checksum
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] actualChecksum = md.digest(decryptedBytes);
            if (!MessageDigest.isEqual(record.checksum(), actualChecksum)) {
                throw new ClassSecurityException("SHA-256 checksum verification failed for class " + name);
            }

            // Define class in JVM
            Class<?> definedClass = defineClass(name, decryptedBytes, 0, decryptedBytes.length);

            // Store in ConcurrentHashMap cache
            classCache.put(name, definedClass);

            // Audit logging
            repository.logAuditEvent(name, "SUCCESS", "Decrypted in RAM, SHA-256 verified, class defined.");

            return definedClass;

        } catch (ClassNotFoundException e) {
            repository.logAuditEvent(name, "FAILED", e.getMessage());
            throw e;
        } catch (DecryptionException | ClassSecurityException e) {
            repository.logAuditEvent(name, "SECURITY_ALERT", e.getMessage());
            throw new ClassNotFoundException("Security or Decryption failure for " + name, e);
        } catch (Exception e) {
            repository.logAuditEvent(name, "ERROR", e.getMessage());
            throw new ClassNotFoundException("Unexpected failure loading class " + name, e);
        } finally {
            // CRITICAL SECURITY REQUIREMENT: Scrub heap memory immediately after defineClass()
            if (decryptedBytes != null) {
                Arrays.fill(decryptedBytes, (byte) 0);
            }
        }
    }

    /**
     * Returns the size of the in-memory class cache.
     */
    public int getCachedClassCount() {
        return classCache.size();
    }
}
