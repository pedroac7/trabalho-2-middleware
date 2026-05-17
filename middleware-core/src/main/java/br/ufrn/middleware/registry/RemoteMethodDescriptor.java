package br.ufrn.middleware.registry;

import br.ufrn.middleware.identification.ObjectId;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class RemoteMethodDescriptor {
    private final String componentName;
    private final ObjectId objectId;
    private final String httpMethod;
    private final String path;
    private final Supplier<Object> targetProvider;
    private final Method javaMethod;

    public RemoteMethodDescriptor(
            String componentName,
            String httpMethod,
            String path,
            Object targetInstance,
            Method javaMethod
    ) {
        this(componentName, httpMethod, path, () -> requireTargetInstance(targetInstance), javaMethod);
    }

    public RemoteMethodDescriptor(
            String componentName,
            String httpMethod,
            String path,
            Supplier<Object> targetProvider,
            Method javaMethod
    ) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name must not be null or blank.");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or blank.");
        }
        if (targetProvider == null) {
            throw new IllegalArgumentException("Target provider must not be null.");
        }
        if (javaMethod == null) {
            throw new IllegalArgumentException("Java method must not be null.");
        }

        this.componentName = componentName;
        this.objectId = ObjectId.of(componentName);
        this.httpMethod = httpMethod.trim().toUpperCase();
        this.path = normalizePath(path);
        this.targetProvider = targetProvider;
        this.javaMethod = javaMethod;
        this.javaMethod.setAccessible(true);
    }

    public String getComponentName() {
        return componentName;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Object getTargetInstance() {
        return targetProvider.get();
    }

    public Method getJavaMethod() {
        return javaMethod;
    }

    private static String normalizePath(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("/+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Object requireTargetInstance(Object targetInstance) {
        if (targetInstance == null) {
            throw new IllegalArgumentException("Target instance must not be null.");
        }
        return targetInstance;
    }
}
