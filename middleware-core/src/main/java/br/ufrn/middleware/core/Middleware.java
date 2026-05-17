package br.ufrn.middleware.core;

import br.ufrn.middleware.invoker.Invoker;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

public class Middleware {
    private final RemoteObjectRegistry registry;
    private final Invoker invoker;

    public Middleware() {
        this.registry = new RemoteObjectRegistry();
        this.invoker = new Invoker();
    }

    public Middleware register(Object targetInstance) {
        registry.register(targetInstance);
        return this;
    }

    public RemoteObjectRegistry getRegistry() {
        return registry;
    }

    public Invoker getInvoker() {
        return invoker;
    }
}
