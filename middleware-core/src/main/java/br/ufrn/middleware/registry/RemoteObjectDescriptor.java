package br.ufrn.middleware.registry;

import java.util.List;

public final class RemoteObjectDescriptor {
    private final String componentName;
    private final Object targetInstance;
    private final Class<?> targetClass;
    private final List<RemoteMethodDescriptor> methods;

    public RemoteObjectDescriptor(
            String componentName,
            Object targetInstance,
            Class<?> targetClass,
            List<RemoteMethodDescriptor> methods
    ) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name must not be null or blank.");
        }
        if (targetInstance == null) {
            throw new IllegalArgumentException("Target instance must not be null.");
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class must not be null.");
        }
        if (methods == null) {
            throw new IllegalArgumentException("Methods list must not be null.");
        }

        this.componentName = componentName;
        this.targetInstance = targetInstance;
        this.targetClass = targetClass;
        this.methods = List.copyOf(methods);
    }

    public String getComponentName() {
        return componentName;
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public List<RemoteMethodDescriptor> getMethods() {
        return methods;
    }
}
