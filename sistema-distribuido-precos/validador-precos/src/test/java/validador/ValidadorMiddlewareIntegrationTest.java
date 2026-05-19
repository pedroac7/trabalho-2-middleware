package validador;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import org.junit.jupiter.api.Test;
import validador.service.ValidadorService;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorMiddlewareIntegrationTest {

    @Test
    void shouldValidatePrecoViaMiddlewareHttpEndpoint() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new ValidadorService());
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/validador");

            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.post(reference, "/validar", "{\"ativo\":\"PETR4\",\"valor\":35.50,\"timestamp\":1710000000000}")
            );

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("\"success\":true"));
            assertTrue(response.getBody().contains("\"valid\":true"));
            assertTrue(response.getBody().contains("VALIDO"));
        } finally {
            middleware.stop();
        }
    }

    @Test
    void shouldReturnBusinessValidationErrorForInvalidPayload() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new ValidadorService());
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/validador");

            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.post(reference, "/validar", "{\"ativo\":\"PETR4\",\"valor\":0,\"timestamp\":1710000000000}")
            );

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("\"success\":true"));
            assertTrue(response.getBody().contains("\"valid\":false"));
            assertTrue(response.getBody().contains("VALOR_INVALIDO"));
        } finally {
            middleware.stop();
        }
    }

    private int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
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