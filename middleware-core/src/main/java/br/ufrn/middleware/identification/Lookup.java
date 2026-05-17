package br.ufrn.middleware.identification;

import br.ufrn.middleware.registry.RemoteMethodDescriptor;
import br.ufrn.middleware.registry.RemoteObjectDescriptor;

import java.util.Optional;

public interface Lookup {
    Optional<RemoteObjectDescriptor> lookup(ObjectId objectId);

    Optional<RemoteMethodDescriptor> lookupMethod(String httpMethod, String path);
}
