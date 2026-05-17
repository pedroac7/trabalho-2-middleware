package br.ufrn.middleware.core;

import br.ufrn.middleware.binding.ParameterBinder;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.invoker.Invoker;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

public class Broker {
    private final RemoteObjectRegistry registry;
    private final ParameterBinder parameterBinder;
    private final Invoker invoker;

    public Broker(RemoteObjectRegistry registry, ParameterBinder parameterBinder, Invoker invoker) {
        if (registry == null) {
            throw new IllegalArgumentException("RemoteObjectRegistry must not be null.");
        }
        if (parameterBinder == null) {
            throw new IllegalArgumentException("ParameterBinder must not be null.");
        }
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker must not be null.");
        }

        this.registry = registry;
        this.parameterBinder = parameterBinder;
        this.invoker = invoker;
    }

    public InvocationResponse handle(InvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("InvocationRequest must not be null.");
        }

        try {
            RemoteMethodDescriptor descriptor = registry.findMethod(request.getHttpMethod(), request.getPath())
                    .orElseThrow(() -> new RemoteMethodNotFoundException(
                            "Remote method not found for route "
                                    + request.getHttpMethod()
                                    + " "
                                    + request.getPath()
                                    + "."
                    ));

            Object[] args = parameterBinder.bind(descriptor, request.getQueryParams(), request.getBody());
            Object result = invoker.invoke(descriptor, args);
            return InvocationResponse.success(result);
        } catch (RuntimeException exception) {
            if (exception instanceof RemotingException) {
                throw exception;
            }
            throw new RemotingException("Unexpected error while handling invocation request.", exception);
        }
    }
}
