package gateway;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import gateway.client.RepositorioClient;
import gateway.client.ValidadorClient;
import gateway.model.PrecoPayload;
import gateway.model.StorageClientResult;
import gateway.model.ValidationClientResult;
import gateway.service.GatewayService;
import heartbeat.HeartbeatReceiver;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayMiddlewareIntegrationTest {

    @Test
    void shouldInvokeGatewayServiceViaMiddlewareAndReturnSuccessEnvelope() throws Exception {
        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(9090, "http");
        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
        receiver.getValidadores().put("127.0.0.1:8081", System.currentTimeMillis());
        receiver.getRepositorios().put("127.0.0.1:8082", System.currentTimeMillis());

        FakeValidadorClient validadorClient = new FakeValidadorClient(ValidationClientResult.accepted());
        FakeRepositorioClient repositorioClient = new FakeRepositorioClient(StorageClientResult.success("ARMAZENADO"));
        GatewayService gatewayService = new GatewayService(0, receiver, validadorClient, repositorioClient);

        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(gatewayService);
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/gateway");
            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.post(reference, "/precos", "{\"ativo\":\"PETR4\",\"valor\":35.50,\"timestamp\":1710000000000}")
            );

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("\"success\":true"));
            assertTrue(response.getBody().contains("\"mensagem\":\"ARMAZENADO\""));
            assertTrue(validadorClient.called);
            assertEquals("127.0.0.1", validadorClient.lastHost);
            assertEquals(8081, validadorClient.lastPort);
            assertTrue(repositorioClient.called);
            assertEquals("127.0.0.1", repositorioClient.lastHost);
            assertEquals(8082, repositorioClient.lastPort);
        } finally {
            middleware.stop();
            receiver.getValidadores().clear();
            receiver.getRepositorios().clear();
        }
    }

    @Test
    void shouldReturnBusinessErrorInsideMiddlewareSuccessEnvelope() throws Exception {
        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(9090, "http");
        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
        receiver.getValidadores().put("127.0.0.1:8081", System.currentTimeMillis());
        receiver.getRepositorios().put("127.0.0.1:8082", System.currentTimeMillis());

        FakeValidadorClient validadorClient = new FakeValidadorClient(ValidationClientResult.invalid("VALOR_INVALIDO"));
        FakeRepositorioClient repositorioClient = new FakeRepositorioClient(StorageClientResult.success("ARMAZENADO"));
        GatewayService gatewayService = new GatewayService(0, receiver, validadorClient, repositorioClient);

        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(gatewayService);
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/gateway");
            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.post(reference, "/precos", "{\"ativo\":\"PETR4\",\"valor\":0,\"timestamp\":1710000000000}")
            );

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getBody().contains("\"success\":true"));
            assertTrue(response.getBody().contains("\"statusCode\":400"));
            assertTrue(response.getBody().contains("VALOR_INVALIDO"));
            assertTrue(validadorClient.called);
            assertTrue(!repositorioClient.called);
        } finally {
            middleware.stop();
            receiver.getValidadores().clear();
            receiver.getRepositorios().clear();
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
        RemoteInvocationResponse get() throws Exception;
    }

    private static final class FakeValidadorClient implements ValidadorClient {
        private final ValidationClientResult response;
        private boolean called;
        private String lastHost;
        private int lastPort;

        private FakeValidadorClient(ValidationClientResult response) {
            this.response = response;
        }

        @Override
        public ValidationClientResult validar(String host, int port, PrecoPayload preco) {
            this.called = true;
            this.lastHost = host;
            this.lastPort = port;
            return response;
        }
    }

    private static final class FakeRepositorioClient implements RepositorioClient {
        private final StorageClientResult response;
        private boolean called;
        private String lastHost;
        private int lastPort;

        private FakeRepositorioClient(StorageClientResult response) {
            this.response = response;
        }

        @Override
        public StorageClientResult armazenar(String host, int port, PrecoPayload preco) {
            this.called = true;
            this.lastHost = host;
            this.lastPort = port;
            return response;
        }
    }
}
