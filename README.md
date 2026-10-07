# Phantasm — Dynamic In-Memory Bytecode Decryption And Execution

Phantasm is a production-grade, cryptographically secure binary protection solution for Java application bytecode. It encrypts compiled `.class` files, stores them as authenticated BLOBs inside an embedded **SQLite** database (`phantasm.db`), decrypts them exclusively within volatile JVM memory (RAM) at runtime using **AES-256-GCM**, validates integrity via **SHA-256**, scrubs memory buffers immediately after loading, and executes methods reflectively via a generic `TaskRunner<T>` engine.

---

## 5 Core Java Academic Modules Implemented

### 1. Introduction to OOP and Java Fundamentals
- Clean, structured package hierarchy under `com.phantasm.*`.
- Strict encapsulation, access modifiers (`private`, `protected`, `public`), constructors, record types (`EncryptedClassRecord`), and utility methods.

### 2. Inheritance and Interfaces
- Interface [`CryptoProvider`](src/com/phantasm/crypto/CryptoProvider.java) defining cryptographic contracts (`encrypt`, `decrypt`, `generateKey`, `generateIv`).
- Implementation [`AesGcmCryptoProvider`](src/com/phantasm/crypto/AesGcmCryptoProvider.java) implementing `CryptoProvider` using `AES/GCM/NoPadding`.
- Custom ClassLoader [`PhantasmClassLoader`](src/com/phantasm/loader/PhantasmClassLoader.java) extending `java.lang.ClassLoader`.

### 3. Exception Handling and I/O
- Custom exception hierarchy:
  - [`DecryptionException`](src/com/phantasm/exception/DecryptionException.java) (Checked)
  - [`KeyNotFoundException`](src/com/phantasm/exception/KeyNotFoundException.java) (Checked)
  - [`ClassSecurityException`](src/com/phantasm/exception/ClassSecurityException.java) (Checked)
- Full `try-with-resources` resource safety across database connections and streams (`DataInputStream`, `ByteArrayInputStream`, `PreparedStatement`).

### 4. Multithreading and Generic Programming
- Generic task execution engine [`TaskRunner<T>`](src/com/phantasm/execution/TaskRunner.java) reflecting methods on dynamically loaded target objects and returning type-safe values of type `T`.
- Concurrent class-loading pipeline [`ParallelClassLoadingPipeline`](src/com/phantasm/execution/ParallelClassLoadingPipeline.java) using `ExecutorService` (FixedThreadPool), `Callable<Class<?>>`, and `Future<Class<?>>` to decrypt and load target classes in parallel across worker threads.

### 5. Collections Framework & Database Connectivity (JDBC)
- **In-Memory Cache**: `ConcurrentHashMap<String, Class<?>>` inside `PhantasmClassLoader` to prevent redundant decryption cycles.
- **Embedded SQLite JDBC**: [`BytecodeRepository`](src/com/phantasm/db/BytecodeRepository.java) using `DriverManager`, `PreparedStatement`, and `ResultSet`.
- **BLOB Storage**: Encrypted class binaries stored in `encrypted_classes` SQLite table.
- **Execution Audit Logging**: Database table `audit_logs` tracking timestamps, class names, security events, and execution status.

---

## Complete Project Structure

```text
phantasm/
├── pom.xml
├── README.md
├── run_demo.sh
└── src/
    └── com/
        └── phantasm/
            ├── crypto/
            │   ├── CryptoProvider.java
            │   └── AesGcmCryptoProvider.java
            ├── exception/
            │   ├── DecryptionException.java
            │   ├── KeyNotFoundException.java
            │   └── ClassSecurityException.java
            ├── db/
            │   └── BytecodeRepository.java
            ├── loader/
            │   └── PhantasmClassLoader.java
            ├── execution/
            │   ├── TaskRunner.java
            │   └── ParallelClassLoadingPipeline.java
            ├── demo/
            │   ├── SecretLogic.java
            │   └── AnalyticsEngine.java
            ├── tools/
            │   └── Encryptor.java
            └── launcher/
                └── PhantasmLauncher.java
```

---

## Workflow & Execution Flow (`PhantasmLauncher.java`)

1. **Setup Database**: Initializes SQLite database `phantasm.db` and creates tables `encrypted_classes` and `audit_logs`.
2. **Encrypt & Store Payload**: Runs `Encryptor` to transform compiled target bytecode (`SecretLogic.class` & `AnalyticsEngine.class`) into authenticated AES-256-GCM BLOB records stored in SQLite.
3. **Concurrent Multithreaded Loading**: `ParallelClassLoadingPipeline` launches `ExecutorService` worker threads to fetch BLOB records, decrypt in RAM via `PhantasmClassLoader`, and register classes in the JVM.
4. **Heap Memory Scrubbing**: Immediately after `defineClass()`, `java.util.Arrays.fill(decryptedBytes, (byte) 0)` zeroes out raw byte arrays inside a `finally` block to mitigate heap dumping attacks.
5. **Generic Reflection Execution**: Reflectively calls target methods via generic `TaskRunner<T>` (`secretMessage`, `validateLicense`, `computeDiscountedPrice`, `calculateSystemHealthScore`, etc.).
6. **Audit Log Inspection**: Retrieves execution history from SQLite `audit_logs` table and displays timestamped audit events.

---

## Build & Execution Instructions

### Option 1: Maven Execution (Recommended)

```bash
mvn clean compile exec:java -Dexec.mainClass="com.phantasm.launcher.PhantasmLauncher"
```

### Option 2: Automated Bash Script

```bash
bash run_demo.sh
```

### Option 3: Arbitrary `.class` File Path CLI (Encrypt & Execute Any Class)

You can pass **any arbitrary compiled `.class` file path** directly to `PhantasmLauncher`. Phantasm will auto-detect the fully qualified class name from the bytecode, encrypt it into SQLite, delete the disk `.class` file, decrypt it strictly in RAM, and reflectively execute its methods:

```powershell
# 1. Encrypt and run any compiled .class file
java -cp "build;lib/sqlite-jdbc-3.36.0.3.jar" com.phantasm.launcher.PhantasmLauncher path/to/YourCustomClass.class

# 2. Execute a specific method on an arbitrary class
java -cp "build;lib/sqlite-jdbc-3.36.0.3.jar" com.phantasm.launcher.PhantasmLauncher path/to/YourCustomClass.class myMethodName

# 3. Encrypt only (without deleting or executing)
java -cp "build;lib/sqlite-jdbc-3.36.0.3.jar" com.phantasm.launcher.PhantasmLauncher --encrypt path/to/YourCustomClass.class
```

---

## Verification & Security Highlights

- **AES-256-GCM Encryption**: NIST-standard 256-bit key with 12-byte IV and 128-bit authentication tag (`AES/GCM/NoPadding`).
- **SHA-256 Bytecode Integrity**: Integrity hash verified before calling `defineClass()`.
- **Zero-Disk Plaintext**: Bytecode decrypted solely inside volatile RAM buffers.
- **Immediate Heap Scrubbing**: Wipes `byte[]` buffers immediately after JVM definition.
- **Thread Safety**: Multithreaded pipeline with `ConcurrentHashMap` thread-safe caching.
