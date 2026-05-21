package br.ufrn.middleware.lifecycle;

import br.ufrn.middleware.annotations.Lifecycle;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.core.Middleware;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedLifecycleManagementTest {

    @RemoteComponent("contadorLazy")
    @Lifecycle(LifecycleType.LAZY_ACQUISITION)
    static class ContadorLazyService {
        static int constructorCalls = 0;
        private int contador = 0;

        public ContadorLazyService() {
            constructorCalls++;
        }

        @RemoteMethod(method = "GET", path = "/incrementar")
        public int incrementar() {
            contador++;
            return contador;
        }
    }

    @RemoteComponent("contadorPool")
    @Lifecycle(LifecycleType.POOLING)
    static class ContadorPoolService {
        static int constructorCalls = 0;

        public ContadorPoolService() {
            constructorCalls++;
        }

        @RemoteMethod(method = "GET", path = "/id")
        public int id() {
            return System.identityHashCode(this);
        }
    }

    @Test
    void lazyAcquisitionDoesNotInstantiateDuringRegister() {
        ContadorLazyService.constructorCalls = 0;

        Middleware middleware = new Middleware();
        middleware.register(ContadorLazyService.class);

        assertEquals(0, ContadorLazyService.constructorCalls);

        InvocationResponse first = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorLazy/incrementar",
                Map.of(),
                null
        ));
        assertEquals(1, first.getBody());
        assertEquals(1, ContadorLazyService.constructorCalls);

        InvocationResponse second = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorLazy/incrementar",
                Map.of(),
                null
        ));
        assertEquals(2, second.getBody());
        assertEquals(1, ContadorLazyService.constructorCalls);
    }

    @Test
    void lazyAcquisitionReusesSameInstance() {
        ContadorLazyService.constructorCalls = 0;

        Middleware middleware = new Middleware();
        middleware.register(ContadorLazyService.class);

        InvocationResponse first = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorLazy/incrementar",
                Map.of(),
                null
        ));

        InvocationResponse second = middleware.getBroker().handle(new InvocationRequest(
                "GET",
                "/contadorLazy/incrementar",
                Map.of(),
                null
        ));

        assertEquals(1, first.getBody());
        assertEquals(2, second.getBody());
    }

    @Test
    void poolingCreatesDefaultPoolDuringRegister() {
        ContadorPoolService.constructorCalls = 0;

        Middleware middleware = new Middleware();
        middleware.register(ContadorPoolService.class);

        assertEquals(5, ContadorPoolService.constructorCalls);
    }

    @Test
    void poolingUsesLimitedSetOfInstances() {
        ContadorPoolService.constructorCalls = 0;

        Middleware middleware = new Middleware();
        middleware.register(ContadorPoolService.class);

        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            InvocationResponse response = middleware.getBroker().handle(new InvocationRequest(
                    "GET",
                    "/contadorPool/id",
                    Map.of(),
                    null
            ));
            ids.add((Integer) response.getBody());
        }

        assertTrue(ids.size() <= 5);
        assertTrue(ids.size() > 1);
    }
}
