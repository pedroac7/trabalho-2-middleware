package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpBodyDtoIntegrationTest {

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
    }

    @Test
    void shouldBindBodyDtoOverHttpAndInvokeService() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new ProdutoService());
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/produtos");

            RemoteInvocationResponse response = invokeWithRetry(
                    () -> requestor.post(reference, "/criar", "{\"id\":10,\"nome\":\"Arroz\"}")
            );

            assertTrue(response.getBody().contains("\"success\":true"));
            assertTrue(response.getBody().contains("10:Arroz"));
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
