package br.ufrn.middleware.identification;

import br.ufrn.middleware.registry.RemoteMethodDescriptor;
import br.ufrn.middleware.registry.RemoteObjectDescriptor;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

import java.util.Optional;

public class LocalLookup implements Lookup {
    private final RemoteObjectRegistry registry;

    public LocalLookup(RemoteObjectRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("RemoteObjectRegistry must not be null.");
        }
        this.registry = registry;
    }

    @Override
    public Optional<RemoteObjectDescriptor> lookup(ObjectId objectId) {
        if (objectId == null) {
            return Optional.empty();
        }
        return registry.findComponent(objectId.getValue());
    }

    @Override
    public Optional<RemoteMethodDescriptor> lookupMethod(String httpMethod, String path) {
        return registry.findMethod(httpMethod, path);
    }
}
