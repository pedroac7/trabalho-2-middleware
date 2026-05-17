package br.ufrn.middleware.registry;

import br.ufrn.middleware.identification.ObjectId;
import br.ufrn.middleware.lifecycle.LifecycleType;

import java.util.List;

public final class RemoteObjectDescriptor {
    private final String componentName;
    private final ObjectId objectId;
    private final Object targetInstance;
    private final Class<?> targetClass;
    private final LifecycleType lifecycleType;
    private final List<RemoteMethodDescriptor> methods;

    public RemoteObjectDescriptor(
            String componentName,
            Object targetInstance,
            Class<?> targetClass,
            LifecycleType lifecycleType,
            List<RemoteMethodDescriptor> methods
    ) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name must not be null or blank.");
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class must not be null.");
        }
        if (lifecycleType == null) {
            throw new IllegalArgumentException("Lifecycle type must not be null.");
        }
        if (methods == null) {
            throw new IllegalArgumentException("Methods list must not be null.");
        }

        this.componentName = componentName;
        this.objectId = ObjectId.of(componentName);
        this.targetInstance = targetInstance;
        this.targetClass = targetClass;
        this.lifecycleType = lifecycleType;
        this.methods = List.copyOf(methods);
    }

    public String getComponentName() {
        return componentName;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public LifecycleType getLifecycleType() {
        return lifecycleType;
    }

    public List<RemoteMethodDescriptor> getMethods() {
        return methods;
    }
}
