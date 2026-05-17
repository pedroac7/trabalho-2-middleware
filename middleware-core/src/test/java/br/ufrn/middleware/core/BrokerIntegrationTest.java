package br.ufrn.middleware.core;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrokerIntegrationTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {

        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }

        @RemoteMethod(method = "GET", path = "/eco")
        public String eco(@Param("valor") String valor) {
            return valor;
        }

        @RemoteMethod(method = "POST", path = "/body")
        public String body(@Body String body) {
            return body;
        }
    }

    @Test
    void deveRegistrarComponenteEInvocarSomaViaBroker() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/soma",
                Map.of("a", "10", "b", "20"),
                null
        );

        InvocationResponse response = middleware.getBroker().handle(request);

        assertEquals(200, response.getStatusCode());
        assertEquals(30, response.getBody());
    }

    @Test
    void deveInvocarMetodoComParamString() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/eco",
                Map.of("valor", "teste"),
                null
        );

        InvocationResponse response = middleware.getBroker().handle(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("teste", response.getBody());
    }

    @Test
    void deveInvocarMetodoComBodyString() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "POST",
                "/calculadora/body",
                Map.of(),
                "conteudo"
        );

        InvocationResponse response = middleware.getBroker().handle(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("conteudo", response.getBody());
    }

    @Test
    void deveLancarRemoteMethodNotFoundExceptionQuandoRotaNaoExistir() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/inexistente",
                Map.of(),
                null
        );

        assertThrows(RemoteMethodNotFoundException.class, () -> middleware.getBroker().handle(request));
    }

    @Test
    void deveLancarBindingExceptionQuandoFaltarParametroPrimitivo() {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/soma",
                Map.of("a", "10"),
                null
        );

        assertThrows(BindingException.class, () -> middleware.getBroker().handle(request));
    }
}
