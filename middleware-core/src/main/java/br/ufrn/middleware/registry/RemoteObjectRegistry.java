package br.ufrn.middleware.registry;

import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteObjectRegistry {
    private final Map<String, RemoteObjectDescriptor> componentsByName = new ConcurrentHashMap<>();
    private final Map<String, RemoteMethodDescriptor> methodsByRoute = new ConcurrentHashMap<>();

    public void register(Object targetInstance) {
        if (targetInstance == null) {
            throw new IllegalArgumentException("Target instance must not be null.");
        }

        Class<?> targetClass = targetInstance.getClass();
        RemoteComponent remoteComponent = targetClass.getAnnotation(RemoteComponent.class);
        if (remoteComponent == null) {
            throw new IllegalArgumentException(
                    "Class " + targetClass.getName() + " is not annotated with @RemoteComponent."
            );
        }

        String componentName = normalizeComponentName(remoteComponent.value());
        if (componentsByName.containsKey(componentName)) {
            throw new IllegalStateException(
                    "A component with name '" + componentName + "' is already registered."
            );
        }

        List<RemoteMethodDescriptor> methodDescriptors = new ArrayList<>();
        Map<String, RemoteMethodDescriptor> routesToRegister = new LinkedHashMap<>();

        for (Method method : targetClass.getDeclaredMethods()) {
            RemoteMethod remoteMethod = method.getAnnotation(RemoteMethod.class);
            if (remoteMethod == null) {
                continue;
            }

            String httpMethod = normalizeHttpMethod(remoteMethod.method());
            String fullPath = buildFullPath(componentName, remoteMethod.path());
            String routeKey = buildRouteKey(httpMethod, fullPath);

            if (routesToRegister.containsKey(routeKey) || methodsByRoute.containsKey(routeKey)) {
                throw new IllegalStateException(
                        "Route already registered: " + routeKey
                );
            }

            RemoteMethodDescriptor descriptor = new RemoteMethodDescriptor(
                    componentName,
                    httpMethod,
                    fullPath,
                    targetInstance,
                    method
            );

            routesToRegister.put(routeKey, descriptor);
            methodDescriptors.add(descriptor);
        }

        if (methodDescriptors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Component '" + componentName + "' does not declare any @RemoteMethod."
            );
        }

        List<String> insertedRouteKeys = new ArrayList<>();
        for (Map.Entry<String, RemoteMethodDescriptor> entry : routesToRegister.entrySet()) {
            RemoteMethodDescriptor previous = methodsByRoute.putIfAbsent(entry.getKey(), entry.getValue());
            if (previous != null) {
                for (String insertedKey : insertedRouteKeys) {
                    methodsByRoute.remove(insertedKey);
                }
                throw new IllegalStateException("Route already registered: " + entry.getKey());
            }
            insertedRouteKeys.add(entry.getKey());
        }

        RemoteObjectDescriptor remoteObjectDescriptor = new RemoteObjectDescriptor(
                componentName,
                targetInstance,
                targetClass,
                methodDescriptors
        );

        RemoteObjectDescriptor existing = componentsByName.putIfAbsent(componentName, remoteObjectDescriptor);
        if (existing != null) {
            for (String insertedKey : insertedRouteKeys) {
                methodsByRoute.remove(insertedKey);
            }
            throw new IllegalStateException(
                    "A component with name '" + componentName + "' is already registered."
            );
        }
    }

    public Optional<RemoteObjectDescriptor> findComponent(String componentName) {
        if (componentName == null || componentName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(componentsByName.get(normalizeComponentName(componentName)));
    }

    public Optional<RemoteMethodDescriptor> findMethod(String httpMethod, String path) {
        if (httpMethod == null || httpMethod.isBlank() || path == null || path.isBlank()) {
            return Optional.empty();
        }
        String routeKey = buildRouteKey(normalizeHttpMethod(httpMethod), normalizePath(path));
        return Optional.ofNullable(methodsByRoute.get(routeKey));
    }

    public List<RemoteObjectDescriptor> getComponents() {
        return List.copyOf(componentsByName.values());
    }

    public List<RemoteMethodDescriptor> getMethods() {
        return List.copyOf(methodsByRoute.values());
    }

    private static String normalizeComponentName(String componentName) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name must not be null or blank.");
        }

        String normalized = componentName.trim().replaceAll("/+", "/");
        normalized = normalized.replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Component name must not be empty.");
        }
        return normalized;
    }

    private static String normalizeHttpMethod(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank.");
        }
        return httpMethod.trim().toUpperCase();
    }

    private static String buildFullPath(String componentName, String methodPath) {
        if (methodPath == null || methodPath.isBlank()) {
            throw new IllegalArgumentException("Remote method path must not be null or blank.");
        }
        return normalizePath("/" + componentName + "/" + methodPath);
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

    private static String buildRouteKey(String httpMethod, String path) {
        return httpMethod + " " + path;
    }
}
