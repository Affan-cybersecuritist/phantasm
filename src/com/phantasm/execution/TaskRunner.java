package com.phantasm.execution;

import java.lang.reflect.Method;

/**
 * Generic execution engine for reflectively calling methods on dynamically loaded objects.
 * Part of Core Academic Module 4: Multithreading and Generic Programming.
 *
 * @param <T> Expected return type of the method invocation
 */
public class TaskRunner<T> {

    private final Object instance;
    private final Method method;

    public TaskRunner(Object instance, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        if (instance == null) {
            throw new IllegalArgumentException("Target instance cannot be null.");
        }
        this.instance = instance;
        this.method = instance.getClass().getMethod(methodName, parameterTypes);
    }

    /**
     * Executes the target method reflectively and casts the return value to generic type T.
     *
     * @param args arguments to pass to the method
     * @return type-safe result of type T
     */
    @SuppressWarnings("unchecked")
    public T execute(Object... args) throws Exception {
        Object result = method.invoke(instance, args);
        return (T) result;
    }

    public Object getInstance() {
        return instance;
    }

    public Method getMethod() {
        return method;
    }
}
