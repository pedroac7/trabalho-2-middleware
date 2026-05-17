package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.core.InvocationResponse;

public interface ResponseMarshaller {
    String marshal(InvocationResponse response);
}
