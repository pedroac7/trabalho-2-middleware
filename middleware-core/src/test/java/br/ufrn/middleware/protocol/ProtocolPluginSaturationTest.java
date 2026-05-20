package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolPluginSaturationTest {

    @RemoteComponent("bloqueio")
    static class BlockingService {
        private static CountDownLatch enteredLatch = new CountDownLatch(1);
        private static CountDownLatch releaseLatch = new CountDownLatch(1);

        @RemoteMethod(method = "POST", path = "/hold")
        public String hold(@Body String body) {
            enteredLatch.countDown();
            try {
                releaseLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return body;
        }

        private static void reset() {
            enteredLatch = new CountDownLatch(1);
            releaseLatch = new CountDownLatch(1);
        }

        private static boolean awaitFirstInvocation() throws InterruptedException {
            return enteredLatch.await(2, TimeUnit.SECONDS);
        }

        private static void release() {
            releaseLatch.countDown();
        }
    }

    @Test
    void httpProtocolReturnsServerBusyWhenQueueIsFull() throws Exception {
        int port = findAvailableTcpPort();
        BlockingService.reset();

        Middleware middleware = new Middleware();
        middleware.register(new BlockingService());
        middleware.useProtocol(new HttpProtocolPlugin(port, 1, 1));
        middleware.start();

        try {
            CompletableFuture<String> firstCall =
                    CompletableFuture.supplyAsync(() -> sendHttpPost(port, "primeira"));

            assertTrue(BlockingService.awaitFirstInvocation(), "First request did not reach service in time.");

            CompletableFuture<String> secondCall =
                    CompletableFuture.supplyAsync(() -> sendHttpPost(port, "segunda"));

            Thread.sleep(100);

            String busyResponse = sendHttpPost(port, "terceira");
            assertTrue(busyResponse.contains("HTTP/1.1 503"));
            assertTrue(busyResponse.contains("SERVER_BUSY"));

            BlockingService.release();
            firstCall.get(3, TimeUnit.SECONDS);
            secondCall.get(3, TimeUnit.SECONDS);
        } finally {
            BlockingService.release();
            middleware.stop();
        }
    }

    @Test
    void tcpProtocolReturnsServerBusyWhenQueueIsFull() throws Exception {
        assertQueueSaturation("tcp", findAvailableTcpPort(), port -> new TcpProtocolPlugin(port, 1, 1));
    }

    @Test
    void udpProtocolReturnsServerBusyWhenQueueIsFull() throws Exception {
        assertQueueSaturation("udp", findAvailableUdpPort(), port -> new UdpProtocolPlugin(port, 1, 1));
    }

    private void assertQueueSaturation(String protocol, int port, PluginFactory pluginFactory) throws Exception {
        BlockingService.reset();

        Middleware middleware = new Middleware();
        middleware.register(new BlockingService());
        middleware.useProtocol(pluginFactory.create(port));
        middleware.start();

        try {
            Requestor requestor = new Requestor();
            AbsoluteObjectReference reference =
                    AbsoluteObjectReference.fromUri(protocol + "://localhost:" + port + "/bloqueio");

            CompletableFuture<RemoteInvocationResponse> firstCall =
                    CompletableFuture.supplyAsync(() -> requestor.post(reference, "/hold", "primeira"));

            assertTrue(BlockingService.awaitFirstInvocation(), "First request did not reach service in time.");

            CompletableFuture<RemoteInvocationResponse> secondCall =
                    CompletableFuture.supplyAsync(() -> requestor.post(reference, "/hold", "segunda"));

            Thread.sleep(100);

            RemoteInvocationResponse busyResponse = requestor.post(reference, "/hold", "terceira");
            assertEquals(503, busyResponse.getStatusCode());
            assertTrue(busyResponse.getBody().contains("SERVER_BUSY"));

            BlockingService.release();
            firstCall.get(3, TimeUnit.SECONDS);
            secondCall.get(3, TimeUnit.SECONDS);
        } finally {
            BlockingService.release();
            middleware.stop();
        }
    }

    private String sendHttpPost(int port, String body) {
        try (Socket socket = new Socket("localhost", port)) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            OutputStream outputStream = socket.getOutputStream();
            String request = "POST /bloqueio/hold HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: text/plain; charset=utf-8\r\n"
                    + "Content-Length: " + bodyBytes.length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";

            outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            outputStream.write(bodyBytes);
            outputStream.flush();

            InputStream inputStream = socket.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                response.write(buffer, 0, read);
            }
            return response.toString(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
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

    @FunctionalInterface
    private interface PluginFactory {
        ProtocolPlugin create(int port);
    }
}
