package br.ufrn.middleware.core;

import br.ufrn.middleware.registry.RemoteObjectRegistry;

public class Middleware {
    private final RemoteObjectRegistry registry;

    public Middleware() {
        this.registry = new RemoteObjectRegistry();
    }

    public Middleware register(Object targetInstance) {
        registry.register(targetInstance);
        return this;
    }

    public RemoteObjectRegistry getRegistry() {
        return registry;
    }
}
