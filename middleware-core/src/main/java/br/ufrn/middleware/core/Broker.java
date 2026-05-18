package br.ufrn.middleware.core;

import br.ufrn.middleware.binding.ParameterBinder;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.interceptor.InvocationContext;
import br.ufrn.middleware.interceptor.InvocationInterceptor;
import br.ufrn.middleware.invoker.Invoker;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

import java.util.ArrayList;
import java.util.List;

public class Broker {
    private final RemoteObjectRegistry registry;
    private final ParameterBinder parameterBinder;
    private final Invoker invoker;
    private final List<InvocationInterceptor> interceptors;

    public Broker(RemoteObjectRegistry registry, ParameterBinder parameterBinder, Invoker invoker) {
        this(registry, parameterBinder, invoker, new ArrayList<>());
    }

    public Broker(
            RemoteObjectRegistry registry,
            ParameterBinder parameterBinder,
            Invoker invoker,
            List<InvocationInterceptor> interceptors
    ) {
        if (registry == null) {
            throw new IllegalArgumentException("RemoteObjectRegistry must not be null.");
        }
        if (parameterBinder == null) {
            throw new IllegalArgumentException("ParameterBinder must not be null.");
        }
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker must not be null.");
        }
        if (interceptors == null) {
            throw new IllegalArgumentException("Interceptors list must not be null.");
        }

        this.registry = registry;
        this.parameterBinder = parameterBinder;
        this.invoker = invoker;
        this.interceptors = interceptors;
    }

    public InvocationResponse handle(InvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("InvocationRequest must not be null.");
        }

        InvocationContext context = new InvocationContext(
                request.getRequestId(),
                "unknown",
                request.getHttpMethod(),
                request.getPath()
        );
        List<InvocationInterceptor> currentInterceptors = List.copyOf(interceptors);

        try {
            runBeforeInterceptors(currentInterceptors, context);

            RemoteMethodDescriptor descriptor = registry.findMethod(request.getHttpMethod(), request.getPath())
                    .orElseThrow(() -> new RemoteMethodNotFoundException(
                            "Remote method not found for route "
                                    + request.getHttpMethod()
                                    + " "
                                    + request.getPath()
                                    + "."
                    ));

            context.setComponentName(descriptor.getComponentName());
            context.setRemoteMethodName(descriptor.getJavaMethod().getName());

            Object[] args = parameterBinder.bind(descriptor, request.getQueryParams(), request.getBody());
            Object result = invoker.invoke(descriptor, args);
            InvocationResponse response = InvocationResponse.success(result);

            context.markFinished(response.getStatusCode());
            runAfterInterceptors(currentInterceptors, context);

            return response;
        } catch (Throwable error) {
            context.setError(error);
            if (context.getStatusCode() == null) {
                context.markFinished(500);
            }
            runOnErrorInterceptors(currentInterceptors, context, error);

            if (error instanceof RuntimeException runtimeException) {
                if (runtimeException instanceof RemotingException) {
                    throw runtimeException;
                }
                throw new RemotingException("Unexpected error while handling invocation request.", runtimeException);
            }

            if (error instanceof Error fatalError) {
                throw fatalError;
            }

            throw new RemotingException("Unexpected error while handling invocation request.", error);
        }
    }

    private void runBeforeInterceptors(List<InvocationInterceptor> currentInterceptors, InvocationContext context) {
        for (InvocationInterceptor interceptor : currentInterceptors) {
            try {
                interceptor.before(context);
            } catch (Throwable error) {
                throw new RemotingException(
                        "Interceptor before() failed: " + interceptor.getClass().getName(),
                        error
                );
            }
        }
    }

    private void runAfterInterceptors(List<InvocationInterceptor> currentInterceptors, InvocationContext context) {
        for (InvocationInterceptor interceptor : currentInterceptors) {
            try {
                interceptor.after(context);
            } catch (Throwable error) {
                throw new RemotingException(
                        "Interceptor after() failed: " + interceptor.getClass().getName(),
                        error
                );
            }
        }
    }

    private void runOnErrorInterceptors(
            List<InvocationInterceptor> currentInterceptors,
            InvocationContext context,
            Throwable originalError
    ) {
        for (InvocationInterceptor interceptor : currentInterceptors) {
            try {
                interceptor.onError(context, originalError);
            } catch (Throwable error) {
                throw new RemotingException(
                        "Interceptor onError() failed: " + interceptor.getClass().getName(),
                        error
                );
            }
        }
    }
}
