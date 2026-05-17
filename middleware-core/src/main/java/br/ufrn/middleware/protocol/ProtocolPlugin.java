package br.ufrn.middleware.protocol;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.marshaller.ResponseMarshaller;

public interface ProtocolPlugin {
    String getName();

    void start(Broker broker, ResponseMarshaller responseMarshaller);

    void stop();
}
