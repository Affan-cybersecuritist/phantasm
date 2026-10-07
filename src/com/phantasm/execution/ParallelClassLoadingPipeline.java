package com.phantasm.execution;

import com.phantasm.loader.PhantasmClassLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Concurrent class-loading pipeline using ExecutorService, Callable<Class<?>>, and Future<Class<?>>.
 * Part of Core Academic Module 4: Multithreading and Generic Programming.
 */
public class ParallelClassLoadingPipeline {

    private final PhantasmClassLoader classLoader;
    private final ExecutorService executor;

    public ParallelClassLoadingPipeline(PhantasmClassLoader classLoader, int threadCount) {
        this.classLoader = classLoader;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public ParallelClassLoadingPipeline(PhantasmClassLoader classLoader) {
        this(classLoader, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Loads multiple target classes concurrently across worker threads.
     *
     * @param classNames list of fully qualified class names to decrypt and load
     * @return list of loaded Class<?> objects
     */
    public List<Class<?>> loadClassesConcurrently(List<String> classNames) throws InterruptedException, ExecutionException {
        List<Callable<Class<?>>> tasks = new ArrayList<>();

        for (String name : classNames) {
            tasks.add(() -> {
                System.out.printf("[%s] Asynchronously fetching, decrypting & loading class: %s%n",
                        Thread.currentThread().getName(), name);
                return classLoader.loadClass(name);
            });
        }

        List<Future<Class<?>>> futures = executor.invokeAll(tasks);
        List<Class<?>> loadedClasses = new ArrayList<>();

        for (Future<Class<?>> future : futures) {
            loadedClasses.add(future.get());
        }

        return loadedClasses;
    }

    /**
     * Gracefully shuts down worker threads.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
