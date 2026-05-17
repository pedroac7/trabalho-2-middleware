package br.ufrn.middleware.interceptor;

import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationInterceptorTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {
        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }
    }

    static class FakeInterceptor implements InvocationInterceptor {
        private boolean beforeCalled;
        private boolean afterCalled;
        private boolean onErrorCalled;

        @Override
        public void before(InvocationContext context) {
            beforeCalled = true;
        }

        @Override
        public void after(InvocationContext context) {
            afterCalled = true;
        }

        @Override
        public void onError(InvocationContext context, Throwable error) {
            onErrorCalled = true;
        }
    }

    @Test
    void deveChamarBeforeEAfterEmInvocacaoComSucesso() {
        FakeInterceptor interceptor = new FakeInterceptor();

        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/soma",
                Map.of("a", "10", "b", "20"),
                null
        );

        middleware.getBroker().handle(request);

        assertTrue(interceptor.beforeCalled);
        assertTrue(interceptor.afterCalled);
    }

    @Test
    void deveChamarOnErrorQuandoRotaNaoExiste() {
        FakeInterceptor interceptor = new FakeInterceptor();

        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "GET",
                "/calculadora/inexistente",
                Map.of(),
                null
        );

        assertThrows(RemoteMethodNotFoundException.class, () -> middleware.getBroker().handle(request));
        assertTrue(interceptor.beforeCalled);
        assertTrue(interceptor.onErrorCalled);
    }
}
