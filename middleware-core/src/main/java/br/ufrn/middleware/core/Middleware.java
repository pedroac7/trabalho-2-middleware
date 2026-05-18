package br.ufrn.middleware.core;

import br.ufrn.middleware.binding.ParameterBinder;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.Lookup;
import br.ufrn.middleware.interceptor.InvocationInterceptor;
import br.ufrn.middleware.invoker.Invoker;
import br.ufrn.middleware.marshaller.InvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.ResponseMarshaller;
import br.ufrn.middleware.marshaller.SimpleJsonResponseMarshaller;
import br.ufrn.middleware.marshaller.SimpleTextInvocationRequestMarshaller;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Middleware {
    private final RemoteObjectRegistry registry;
    private final Invoker invoker;
    private final ParameterBinder parameterBinder;
    private final Broker broker;
    private final InvocationRequestMarshaller invocationRequestMarshaller;
    private final ResponseMarshaller responseMarshaller;
    private final Requestor requestor;
    private final List<ProtocolPlugin> protocolPlugins;
    private final List<InvocationInterceptor> interceptors;

    public Middleware() {
        this.registry = new RemoteObjectRegistry();
        this.invoker = new Invoker();
        this.parameterBinder = new ParameterBinder();
        this.interceptors = new CopyOnWriteArrayList<>();
        this.broker = new Broker(registry, parameterBinder, invoker, interceptors);
        this.invocationRequestMarshaller = new SimpleTextInvocationRequestMarshaller();
        this.responseMarshaller = new SimpleJsonResponseMarshaller();
        this.requestor = new Requestor();
        this.protocolPlugins = new CopyOnWriteArrayList<>();
    }

    public Middleware register(Object targetInstance) {
        registry.register(targetInstance);
        return this;
    }

    public Middleware register(Class<?> targetClass) {
        registry.register(targetClass);
        return this;
    }

    public RemoteObjectRegistry getRegistry() {
        return registry;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public ParameterBinder getParameterBinder() {
        return parameterBinder;
    }

    public Broker getBroker() {
        return broker;
    }

    public ResponseMarshaller getResponseMarshaller() {
        return responseMarshaller;
    }

    public InvocationRequestMarshaller getInvocationRequestMarshaller() {
        return invocationRequestMarshaller;
    }

    public Requestor getRequestor() {
        return requestor;
    }

    public Lookup getLookup() {
        return registry.asLookup();
    }

    public Middleware useProtocol(ProtocolPlugin protocolPlugin) {
        if (protocolPlugin == null) {
            throw new IllegalArgumentException("Protocol plugin must not be null.");
        }
        protocolPlugins.add(protocolPlugin);
        return this;
    }

    public Middleware addInterceptor(InvocationInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("Invocation interceptor must not be null.");
        }
        interceptors.add(interceptor);
        return this;
    }

    public void start() {
        for (ProtocolPlugin protocolPlugin : protocolPlugins) {
            protocolPlugin.start(broker, responseMarshaller);
        }
    }

    public void stop() {
        for (ProtocolPlugin protocolPlugin : protocolPlugins) {
            try {
                protocolPlugin.stop();
            } catch (RuntimeException ignored) {
            }
        }
    }
}
