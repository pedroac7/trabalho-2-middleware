package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.error.BindingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonBodyMarshallerTest {

    static class ProdutoDto {
        public int id;
        public String nome;
    }

    private final JsonBodyMarshaller marshaller = new JsonBodyMarshaller();

    @Test
    void shouldDeserializeJsonToDto() {
        Object result = marshaller.unmarshalBody("{\"id\":1,\"nome\":\"Caf\u00E9\"}", ProdutoDto.class);
        ProdutoDto produto = (ProdutoDto) result;

        assertEquals(1, produto.id);
        assertEquals("Caf\u00E9", produto.nome);
    }

    @Test
    void shouldReturnStringWhenTargetTypeIsString() {
        Object result = marshaller.unmarshalBody("conteudo bruto", String.class);
        assertEquals("conteudo bruto", result);
    }

    @Test
    void shouldThrowBindingExceptionForInvalidJson() {
        assertThrows(
                BindingException.class,
                () -> marshaller.unmarshalBody("{\"id\":", ProdutoDto.class)
        );
    }

    @Test
    void shouldThrowBindingExceptionForEmptyBodyWhenTargetIsDto() {
        assertThrows(
                BindingException.class,
                () -> marshaller.unmarshalBody("   ", ProdutoDto.class)
        );
    }
}
