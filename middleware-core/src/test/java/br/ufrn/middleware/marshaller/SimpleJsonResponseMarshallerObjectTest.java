package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.core.InvocationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleJsonResponseMarshallerObjectTest {

    static class ProdutoDto {
        public int id;
        public String nome;

        ProdutoDto(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }
    }

    @Test
    void shouldSerializeObjectBodyWithJackson() {
        SimpleJsonResponseMarshaller marshaller = new SimpleJsonResponseMarshaller();
        InvocationResponse response = InvocationResponse.success(new ProdutoDto(1, "Caf\u00E9"));

        String json = marshaller.marshal(response);

        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"statusCode\":200"));
        assertTrue(json.contains("\"result\""));
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"nome\":\"Caf\u00E9\""));
    }
}
