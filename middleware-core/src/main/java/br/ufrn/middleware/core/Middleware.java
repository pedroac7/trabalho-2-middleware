package br.ufrn.middleware.core;

import br.ufrn.middleware.binding.ParameterBinder;
import br.ufrn.middleware.invoker.Invoker;
import br.ufrn.middleware.marshaller.ResponseMarshaller;
import br.ufrn.middleware.marshaller.SimpleJsonResponseMarshaller;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.registry.RemoteObjectRegistry;

import java.util.ArrayList;
import java.util.List;

public class Middleware {
    private final RemoteObjectRegistry registry;
    private final Invoker invoker;
    private final ParameterBinder parameterBinder;
    private final Broker broker;
    private final ResponseMarshaller responseMarshaller;
    private final List<ProtocolPlugin> protocolPlugins;

    public Middleware() {
        this.registry = new RemoteObjectRegistry();
        this.invoker = new Invoker();
        this.parameterBinder = new ParameterBinder();
        this.broker = new Broker(registry, parameterBinder, invoker);
        this.responseMarshaller = new SimpleJsonResponseMarshaller();
        this.protocolPlugins = new ArrayList<>();
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

    public ParameterBinder getParameterBinder() {
        return parameterBinder;
    }

    public Broker getBroker() {
        return broker;
    }

    public ResponseMarshaller getResponseMarshaller() {
        return responseMarshaller;
    }

    public Middleware useProtocol(ProtocolPlugin protocolPlugin) {
        if (protocolPlugin == null) {
            throw new IllegalArgumentException("Protocol plugin must not be null.");
        }
        protocolPlugins.add(protocolPlugin);
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
