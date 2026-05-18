package br.ufrn.middleware.interceptor;

import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.client.RemoteInvocationRequest;
import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.marshaller.SimpleTextInvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.TextInvocationMessage;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import br.ufrn.middleware.protocol.TcpProtocolPlugin;
import br.ufrn.middleware.protocol.UdpProtocolPlugin;
import org.junit.jupiter.api.Test;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationContextPropagationTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {
        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }
    }

    static class CaptureRequestIdInterceptor implements InvocationInterceptor {
        private final AtomicReference<String> lastRequestId = new AtomicReference<>();

        @Override
        public void before(InvocationContext context) {
            lastRequestId.set(context.getRequestId());
        }
    }

    @Test
    void brokerUsesRequestIdFromInvocationRequest() {
        CaptureRequestIdInterceptor interceptor = new CaptureRequestIdInterceptor();
        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());

        InvocationRequest request = new InvocationRequest(
                "req-123",
                "GET",
                "/calculadora/soma",
                Map.of("a", "10", "b", "20"),
                null
        );

        middleware.getBroker().handle(request);

        assertEquals("req-123", interceptor.lastRequestId.get());
    }

    @Test
    void textMarshallerPreservesRequestId() {
        SimpleTextInvocationRequestMarshaller marshaller = new SimpleTextInvocationRequestMarshaller();
        TextInvocationMessage source = new TextInvocationMessage(
                "req-456",
                "GET",
                "/calculadora/soma",
                Map.of("a", "10", "b", "20"),
                ""
        );

        byte[] bytes = marshaller.marshal(source);
        TextInvocationMessage decoded = marshaller.unmarshal(bytes);

        assertEquals("req-456", decoded.getRequestId());
    }

    @Test
    void tcpClientServerPropagatesRequestId() throws Exception {
        int port = findAvailableTcpPort();
        CaptureRequestIdInterceptor interceptor = new CaptureRequestIdInterceptor();

        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new TcpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("tcp://localhost:" + port + "/calculadora");

            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    "req-tcp-1",
                    reference,
                    "GET",
                    "/soma",
                    Map.of("a", "10", "b", "20"),
                    null
            );

            RemoteInvocationResponse response = invokeWithRetry(() -> requestor.invoke(request));

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals("req-tcp-1", interceptor.lastRequestId.get());
        } finally {
            middleware.stop();
        }
    }

    @Test
    void udpClientServerPropagatesRequestId() throws Exception {
        int port = findAvailableUdpPort();
        CaptureRequestIdInterceptor interceptor = new CaptureRequestIdInterceptor();

        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new UdpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("udp://localhost:" + port + "/calculadora");

            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    "req-udp-1",
                    reference,
                    "GET",
                    "/soma",
                    Map.of("a", "10", "b", "20"),
                    null
            );

            RemoteInvocationResponse response = invokeWithRetry(() -> requestor.invoke(request));

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals("req-udp-1", interceptor.lastRequestId.get());
        } finally {
            middleware.stop();
        }
    }

    @Test
    void httpClientServerPropagatesRequestId() throws Exception {
        int port = findAvailableTcpPort();
        CaptureRequestIdInterceptor interceptor = new CaptureRequestIdInterceptor();

        Middleware middleware = new Middleware();
        middleware.addInterceptor(interceptor);
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new HttpProtocolPlugin(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/calculadora");

            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    "req-http-1",
                    reference,
                    "GET",
                    "/soma",
                    Map.of("a", "10", "b", "20"),
                    null
            );

            RemoteInvocationResponse response = invokeWithRetry(() -> requestor.invoke(request));

            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals("req-http-1", interceptor.lastRequestId.get());
        } finally {
            middleware.stop();
        }
    }

    private int findAvailableTcpPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private int findAvailableUdpPort() throws Exception {
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
