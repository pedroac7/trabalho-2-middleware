package br.ufrn.middleware.marshaller;

public interface InvocationRequestMarshaller {
    byte[] marshal(TextInvocationMessage message);

    TextInvocationMessage unmarshal(byte[] bytes);
}
