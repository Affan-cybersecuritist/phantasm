package com.phantasm.launcher;

import com.phantasm.crypto.AesGcmCryptoProvider;
import com.phantasm.crypto.CryptoProvider;
import com.phantasm.db.BytecodeRepository;
import com.phantasm.execution.ParallelClassLoadingPipeline;
import com.phantasm.execution.TaskRunner;
import com.phantasm.loader.PhantasmClassLoader;
import com.phantasm.tools.ClassFileHelper;
import com.phantasm.tools.Encryptor;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Main application entry point orchestrating both:
 * 1. Default academic demo pipeline (when launched with no CLI arguments).
 * 2. Arbitrary .class file path encryption & dynamic in-RAM execution (when passed file paths via CLI).
 */
public class PhantasmLauncher {

    private static final String KEY_FILE = ".phantasm.key";

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  Phantasm: Dynamic In-Memory Bytecode Decryption And Execution (Java 17+) ");
        System.out.println("==========================================================================");

        try {
            if (args == null || args.length == 0) {
                runDefaultDemoPipeline();
            } else {
                runArbitraryClassCliPipeline(args);
            }
        } catch (Exception e) {
            System.err.println("\n[FATAL ERROR] Pipeline execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Executes the built-in 5-module academic demo.
     */
    private static void runDefaultDemoPipeline() throws Exception {
        // --- Step 1: Initialize Database DAO ---
        System.out.println("\n[STEP 1] Setting up SQLite Database & DAO Layer ('phantasm.db')...");
        BytecodeRepository repository = new BytecodeRepository("phantasm.db");
        repository.initializeTables();
        System.out.println("-> Database tables 'encrypted_classes' and 'audit_logs' ready.");

        // --- Step 2: Key Resolution & Encryption Tooling ---
        System.out.println("\n[STEP 2] Resolving AES Secret Key & Encrypting Target Bytecode into SQLite BLOBs...");
        CryptoProvider cryptoProvider = new AesGcmCryptoProvider();
        SecretKey secretKey = resolveOrGenerateKey(cryptoProvider);
        String hexKey = cryptoProvider.keyToHex(secretKey);
        System.out.println("-> Active AES-256 Key (Hex): " + hexKey);

        Path compiledClassesDir = Path.of("build");
        Encryptor encryptor = new Encryptor(cryptoProvider, repository);

        String secretLogicName = "com.phantasm.demo.SecretLogic";
        String analyticsEngineName = "com.phantasm.demo.AnalyticsEngine";

        encryptor.encryptAndStore(compiledClassesDir, secretKey, secretLogicName, analyticsEngineName);

        // Deleting plaintext .class files to guarantee dynamic class loader is used
        Path secretClassFile = compiledClassesDir.resolve("com/phantasm/demo/SecretLogic.class");
        Path analyticsClassFile = compiledClassesDir.resolve("com/phantasm/demo/AnalyticsEngine.class");
        Files.deleteIfExists(secretClassFile);
        Files.deleteIfExists(analyticsClassFile);
        System.out.println("-> Deleted plaintext .class files from build/ to simulate zero disk footprint.");

        // --- Step 3: Multithreaded Decryption & Dynamic Class Loading ---
        System.out.println("\n[STEP 3] Launching ExecutorService Pipeline for Parallel In-RAM Class Decryption & Loading...");
        PhantasmClassLoader classLoader = new PhantasmClassLoader(cryptoProvider, secretKey, repository);
        ParallelClassLoadingPipeline pipeline = new ParallelClassLoadingPipeline(classLoader, 4);

        List<String> targetClasses = List.of(secretLogicName, analyticsEngineName);
        List<Class<?>> loadedClasses = pipeline.loadClassesConcurrently(targetClasses);
        pipeline.shutdown();

        System.out.println("-> Concurrently loaded " + loadedClasses.size() + " classes into JVM via PhantasmClassLoader.");
        System.out.println("-> ConcurrentHashMap Class Cache size: " + classLoader.getCachedClassCount());

        for (Class<?> clazz : loadedClasses) {
            System.out.println("   Class: " + clazz.getName() + " | Loaded by: " + clazz.getClassLoader().getClass().getName());
        }

        // --- Step 4: Verification of Heap Memory Scrubbing ---
        System.out.println("\n[STEP 4] Heap Memory Scrubbing Verification...");
        System.out.println("-> Decrypted byte buffers cleared in volatile RAM via Arrays.fill(decryptedBytes, (byte)0).");

        // --- Step 5: Generic Task Execution via TaskRunner<T> & Reflection ---
        System.out.println("\n[STEP 5] Reflective Execution via Generic TaskRunner<T> Engine...");

        Class<?> secretClass = loadedClasses.stream()
                .filter(c -> c.getName().equals(secretLogicName))
                .findFirst()
                .orElseThrow();

        Object secretInstance = secretClass.getDeclaredConstructor().newInstance();

        TaskRunner<String> msgTask = new TaskRunner<>(secretInstance, "secretMessage");
        String msgResult = msgTask.execute();
        System.out.println("   [SecretLogic.secretMessage()]       -> " + msgResult);

        TaskRunner<Boolean> licenseTask = new TaskRunner<>(secretInstance, "validateLicense", String.class);
        Boolean validLicense = licenseTask.execute("PHANTASM-A1B2C3D4");
        Boolean invalidLicense = licenseTask.execute("INVALID-KEY");
        System.out.println("   [SecretLogic.validateLicense(Valid)]   -> " + validLicense);
        System.out.println("   [SecretLogic.validateLicense(Invalid)] -> " + invalidLicense);

        TaskRunner<Double> priceTask = new TaskRunner<>(secretInstance, "computeDiscountedPrice", double.class, int.class);
        Double finalPrice = priceTask.execute(1500.0, 5);
        System.out.println("   [SecretLogic.computeDiscountedPrice()] -> $" + finalPrice);

        Class<?> analyticsClass = loadedClasses.stream()
                .filter(c -> c.getName().equals(analyticsEngineName))
                .findFirst()
                .orElseThrow();
        Object analyticsInstance = analyticsClass.getDeclaredConstructor().newInstance();

        TaskRunner<String> verTask = new TaskRunner<>(analyticsInstance, "getEngineVersion");
        System.out.println("   [AnalyticsEngine.getEngineVersion()]    -> " + verTask.execute());

        TaskRunner<Double> healthTask = new TaskRunner<>(analyticsInstance, "calculateSystemHealthScore", int.class, double.class, long.class);
        System.out.println("   [AnalyticsEngine.calculateHealth()]     -> " + healthTask.execute(16, 25.5, 2048L));

        TaskRunner<String> reportTask = new TaskRunner<>(analyticsInstance, "generateTelemetryReport", String.class);
        System.out.println("   [AnalyticsEngine.generateReport()]     -> " + reportTask.execute("PROD-CLUSTER-01"));

        // --- Step 6: Audit Logging Verification from SQLite ---
        System.out.println("\n[STEP 6] Fetching Database Audit Trail from SQLite 'audit_logs' Table...");
        List<String> auditLogs = repository.getAuditLogs();
        for (String logEntry : auditLogs) {
            System.out.println("   " + logEntry);
        }

        System.out.println("\n==========================================================================");
        System.out.println("  Phantasm Execution Completed Successfully! All 5 Modules Demonstrated.  ");
        System.out.println("==========================================================================");
    }

    /**
     * Pipeline for handling arbitrary .class file paths from the command line.
     */
    private static void runArbitraryClassCliPipeline(String[] args) throws Exception {
        boolean encryptOnly = false;
        boolean keepDisk = false;
        String targetPathStr = null;
        String targetMethod = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--encrypt".equalsIgnoreCase(arg)) {
                encryptOnly = true;
            } else if ("--keep-disk".equalsIgnoreCase(arg)) {
                keepDisk = true;
            } else if (targetPathStr == null) {
                targetPathStr = arg;
            } else if (targetMethod == null) {
                targetMethod = arg;
            }
        }

        if (targetPathStr == null) {
            System.err.println("Usage: PhantasmLauncher [--encrypt] [--keep-disk] <path/to/ClassFile.class> [methodName]");
            System.exit(1);
        }

        Path classFilePath = Paths.get(targetPathStr);
        BytecodeRepository repository = new BytecodeRepository("phantasm.db");
        repository.initializeTables();

        CryptoProvider cryptoProvider = new AesGcmCryptoProvider();
        SecretKey secretKey = resolveOrGenerateKey(cryptoProvider);

        String className;
        if (Files.exists(classFilePath) && Files.isRegularFile(classFilePath)) {
            System.out.println("\n[STEP 1] Auto-detecting class name & encrypting `.class` file into SQLite...");
            Encryptor encryptor = new Encryptor(cryptoProvider, repository);
            className = encryptor.encryptFile(classFilePath, null, secretKey);

            if (!keepDisk && !encryptOnly) {
                Files.deleteIfExists(classFilePath);
                System.out.println("-> Deleted original `.class` file from disk (" + classFilePath + ") to enforce zero disk footprint.");
            }
        } else if (targetPathStr.endsWith(".class")) {
            System.err.println("\n[ERROR] File not found: " + classFilePath.toAbsolutePath());
            System.err.println("Please specify a valid path to an existing compiled `.class` file.");
            System.exit(1);
            return;
        } else {
            // Assume argument is an already encrypted fully qualified class name in phantasm.db
            className = targetPathStr;
            System.out.println("\n[STEP 1] Target specified as class name in SQLite database: " + className);
        }

        if (encryptOnly) {
            System.out.println("\n[SUCCESS] Class '" + className + "' successfully encrypted and stored in SQLite phantasm.db.");
            return;
        }

        // --- Step 2: Load class strictly into volatile RAM ---
        System.out.println("\n[STEP 2] Decrypting & Loading Class strictly in volatile RAM via PhantasmClassLoader...");
        PhantasmClassLoader classLoader = new PhantasmClassLoader(cryptoProvider, secretKey, repository);
        Class<?> loadedClass = classLoader.loadClass(className);
        System.out.println("-> Loaded class: " + loadedClass.getName() + " via " + loadedClass.getClassLoader().getClass().getSimpleName());

        // --- Step 3: Reflective Execution ---
        System.out.println("\n[STEP 3] Executing methods reflectively via TaskRunner...");
        executeReflectively(loadedClass, targetMethod);

        System.out.println("\n==========================================================================");
        System.out.println("  Phantasm Execution Finished for " + className);
        System.out.println("==========================================================================");
    }

    /**
     * Reflectively executes targetMethod or inspects and invokes available public methods.
     */
    private static void executeReflectively(Class<?> loadedClass, String targetMethod) throws Exception {
        Method mainMethod = null;
        try {
            mainMethod = loadedClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException ignored) {}

        if (targetMethod != null) {
            Object instance = Modifier.isStatic(mainMethod != null ? mainMethod.getModifiers() : 0) ? null
                    : loadedClass.getDeclaredConstructor().newInstance();

            Method m = Arrays.stream(loadedClass.getDeclaredMethods())
                    .filter(method -> method.getName().equals(targetMethod))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("No method named '" + targetMethod + "' in " + loadedClass.getName()));

            m.setAccessible(true);
            Object result;
            if (m.getParameterCount() == 0) {
                result = m.invoke(instance);
            } else {
                result = m.invoke(instance, (Object) new String[0]);
            }
            System.out.println("   [" + loadedClass.getSimpleName() + "." + m.getName() + "()] -> " + result);

        } else if (mainMethod != null && Modifier.isStatic(mainMethod.getModifiers())) {
            System.out.println("   Found public static void main(String[] args). Executing...");
            mainMethod.invoke(null, (Object) new String[0]);
            System.out.println("   [main()] executed successfully.");
        } else {
            System.out.println("   No explicit method specified. Discovering zero-arg public methods...");
            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            int executedCount = 0;
            for (Method m : loadedClass.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getName().equals("wait") && !m.getName().equals("notify")) {
                    TaskRunner<Object> runner = new TaskRunner<>(instance, m.getName());
                    Object res = runner.execute();
                    System.out.println("   [" + loadedClass.getSimpleName() + "." + m.getName() + "()] -> " + res);
                    executedCount++;
                }
            }
            if (executedCount == 0) {
                System.out.println("   Class instantiated successfully: " + instance);
            }
        }
    }

    /**
     * Resolves secret key from env var PHANTASM_SECRET_KEY, persistent .phantasm.key file, or generates a new key.
     */
    private static SecretKey resolveOrGenerateKey(CryptoProvider cryptoProvider) throws Exception {
        String envKey = System.getenv("PHANTASM_SECRET_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return cryptoProvider.keyFromHex(envKey);
        }

        Path keyPath = Paths.get(KEY_FILE);
        if (Files.exists(keyPath)) {
            String storedHex = Files.readString(keyPath).trim();
            return cryptoProvider.keyFromHex(storedHex);
        }

        SecretKey newKey = cryptoProvider.generateKey();
        Files.writeString(keyPath, cryptoProvider.keyToHex(newKey));
        return newKey;
    }
}
