package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.core.InvocationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleJsonResponseMarshallerTest {

    private final SimpleJsonResponseMarshaller marshaller = new SimpleJsonResponseMarshaller();

    @Test
    void deveSerializarSucessoComNumero() {
        InvocationResponse response = InvocationResponse.success(30);

        String json = marshaller.marshal(response);

        assertEquals("{\"success\":true,\"statusCode\":200,\"result\":30}", json);
    }

    @Test
    void deveSerializarSucessoComString() {
        InvocationResponse response = InvocationResponse.success("teste");

        String json = marshaller.marshal(response);

        assertEquals("{\"success\":true,\"statusCode\":200,\"result\":\"teste\"}", json);
    }

    @Test
    void deveSerializarSucessoComNull() {
        InvocationResponse response = InvocationResponse.success(null);

        String json = marshaller.marshal(response);

        assertEquals("{\"success\":true,\"statusCode\":200,\"result\":null}", json);
    }

    @Test
    void deveSerializarErro() {
        InvocationResponse response = InvocationResponse.error(404, "rota não encontrada");

        String json = marshaller.marshal(response);

        assertEquals("{\"success\":false,\"statusCode\":404,\"error\":\"rota não encontrada\"}", json);
    }

    @Test
    void deveEscaparAspasEmString() {
        InvocationResponse response = InvocationResponse.success("a\"b");

        String json = marshaller.marshal(response);

        assertEquals("{\"success\":true,\"statusCode\":200,\"result\":\"a\\\"b\"}", json);
    }
}
