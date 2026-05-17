package br.ufrn.middleware.lifecycle;

import br.ufrn.middleware.annotations.Lifecycle;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.core.Middleware;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleManagementTest {

    @RemoteComponent("contadorStatic")
    @Lifecycle(LifecycleType.STATIC_INSTANCE)
    static class ContadorStaticService {
        private int contador = 0;

        @RemoteMethod(method = "GET", path = "/incrementar")
        public int incrementar() {
            contador++;
            return contador;
        }
    }

    @RemoteComponent("contadorRequest")
    @Lifecycle(LifecycleType.PER_REQUEST_INSTANCE)
    static class ContadorRequestService {
        private int contador = 0;

        @RemoteMethod(method = "GET", path = "/incrementar")
        public int incrementar() {
            contador++;
            return contador;
        }
    }

    @RemoteComponent("simples")
    static class SimplesService {
        @RemoteMethod(method = "GET", path = "/ping")
        public String ping() {
            return "pong";
        }
    }

    @Test
    void staticInstanceMantemEstadoEntreChamadas() {
        Middleware middleware = new Middleware();
        middleware.register(ContadorStaticService.class);

        InvocationResponse first = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorStatic/incrementar",
                Map.of(),
                null
        ));

        InvocationResponse second = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorStatic/incrementar",
                Map.of(),
                null
        ));

        assertEquals(1, first.getBody());
        assertEquals(2, second.getBody());
    }

    @Test
    void perRequestInstanceNaoMantemEstadoEntreChamadas() {
        Middleware middleware = new Middleware();
        middleware.register(ContadorRequestService.class);

        InvocationResponse first = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorRequest/incrementar",
                Map.of(),
                null
        ));

        InvocationResponse second = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorRequest/incrementar",
                Map.of(),
                null
        ));

        assertEquals(1, first.getBody());
        assertEquals(1, second.getBody());
    }

    @Test
    void registerPorInstanciaContinuaFuncionando() {
        Middleware middleware = new Middleware();
        middleware.register(new SimplesService());

        InvocationResponse response = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/simples/ping",
                Map.of(),
                null
        ));

        assertEquals(200, response.getStatusCode());
        assertEquals("pong", response.getBody());
    }
}
