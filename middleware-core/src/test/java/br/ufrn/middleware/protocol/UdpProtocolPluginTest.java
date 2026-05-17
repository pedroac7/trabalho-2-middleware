package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpProtocolPluginTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {
        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }
    }

    @Test
    void udpPluginReturnsSuccessForRegisteredRoute() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new UdpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("udp://localhost:" + port + "/calculadora");

            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.get(reference, "/soma", Map.of("a", "10", "b", "20"))
            );

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("\"result\":30"));
        } finally {
            middleware.stop();
        }
    }

    @Test
    void udpPluginReturnsNotFoundForUnknownRoute() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new UdpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("udp://localhost:" + port + "/calculadora");

            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.get(reference, "/inexistente", Map.of())
            );

            assertTrue(response.getBody().contains("\"success\":false"));
            assertTrue(response.getBody().contains("\"statusCode\":404"));
        } finally {
            middleware.stop();
        }
    }

    private int findAvailablePort() throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private RemoteInvocationResponse invokeWithRetry(InvocationSupplier supplier) throws Exception {
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                return supplier.get();
            } catch (Exception exception) {
                lastException = exception;
                Thread.sleep(50);
            }
        }
        throw lastException;
    }

    @FunctionalInterface
    private interface InvocationSupplier {
        RemoteInvocationResponse get();
    }
}
