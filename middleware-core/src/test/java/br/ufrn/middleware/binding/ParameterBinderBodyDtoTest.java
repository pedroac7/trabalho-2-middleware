package br.ufrn.middleware.binding;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.error.BindingException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterBinderBodyDtoTest {

    static class ProdutoDto {
        public int id;
        public String nome;
    }

    @RemoteComponent("produtos")
    static class ProdutoService {
        @RemoteMethod(method = "POST", path = "/criar")
        public String criar(@Body ProdutoDto produto) {
            return produto.id + ":" + produto.nome;
        }

        @RemoteMethod(method = "POST", path = "/invalido")
        public String invalido(@Body ProdutoDto a, @Body ProdutoDto b) {
            return "x";
        }
    }

    @Test
    void shouldBindBodyJsonToDtoAndInvokeMethod() {
        Middleware middleware = new Middleware();
        middleware.register(new ProdutoService());

        InvocationRequest request = new InvocationRequest(
                "POST",
                "/produtos/criar",
                Map.of(),
                "{\"id\":10,\"nome\":\"Arroz\"}"
        );

        InvocationResponse response = middleware.getBroker().handle(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("10:Arroz", response.getBody());
    }

    @Test
    void shouldThrowBindingExceptionWhenMethodHasMultipleBodyParameters() {
        Middleware middleware = new Middleware();
        middleware.register(new ProdutoService());

        InvocationRequest request = new InvocationRequest(
                "POST",
                "/produtos/invalido",
                Map.of(),
                "{\"id\":10,\"nome\":\"Arroz\"}"
        );

        assertThrows(BindingException.class, () -> middleware.getBroker().handle(request));
    }
}
