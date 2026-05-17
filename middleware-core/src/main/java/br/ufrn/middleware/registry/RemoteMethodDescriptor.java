package br.ufrn.middleware.registry;

import java.lang.reflect.Method;

public final class RemoteMethodDescriptor {
    private final String componentName;
    private final String httpMethod;
    private final String path;
    private final Object targetInstance;
    private final Method javaMethod;

    public RemoteMethodDescriptor(
            String componentName,
            String httpMethod,
            String path,
            Object targetInstance,
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
        if (targetInstance == null) {
            throw new IllegalArgumentException("Target instance must not be null.");
        }
        if (javaMethod == null) {
            throw new IllegalArgumentException("Java method must not be null.");
        }

        this.componentName = componentName;
        this.httpMethod = httpMethod.trim().toUpperCase();
        this.path = normalizePath(path);
        this.targetInstance = targetInstance;
        this.javaMethod = javaMethod;
        this.javaMethod.setAccessible(true);
    }

    public String getComponentName() {
        return componentName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Object getTargetInstance() {
        return targetInstance;
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
}
