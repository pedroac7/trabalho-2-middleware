package br.ufrn.middleware.protocol;

import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.core.Middleware;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TcpProtocolPluginTest {

    @RemoteComponent("calculadora")
    static class CalculadoraService {
        @RemoteMethod(method = "GET", path = "/soma")
        public int soma(@Param("a") int a, @Param("b") int b) {
            return a + b;
        }
    }

    @Test
    void tcpPluginReturnsSuccessForRegisteredRoute() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new TcpProtocolPlugin(port));

        middleware.start();
        try {
            String response = sendRequest(port, "GET", "/calculadora/soma", "a=10&b=20", null);
            assertTrue(response.contains("\"success\":true"));
            assertTrue(response.contains("\"result\":30"));
        } finally {
            middleware.stop();
        }
    }

    @Test
    void tcpPluginReturnsNotFoundForUnknownRoute() throws Exception {
        int port = findAvailablePort();
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new TcpProtocolPlugin(port));

        middleware.start();
        try {
            String response = sendRequest(port, "GET", "/calculadora/inexistente", "", null);
            assertTrue(response.contains("\"success\":false"));
            assertTrue(response.contains("\"statusCode\":404"));
        } finally {
            middleware.stop();
        }
    }

    private static int findAvailablePort() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private static String sendRequest(
            int port,
            String method,
            String path,
            String query,
            String body
    ) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);

            StringBuilder requestBuilder = new StringBuilder();
            requestBuilder.append("METHOD ").append(method).append('\n');
            requestBuilder.append("PATH ").append(path).append('\n');
            requestBuilder.append("QUERY");
            if (query != null && !query.isEmpty()) {
                requestBuilder.append(' ').append(query);
            }
            requestBuilder.append('\n');
            requestBuilder.append("BODY_LENGTH ").append(bodyBytes.length).append('\n');
            requestBuilder.append('\n');

            outputStream.write(requestBuilder.toString().getBytes(StandardCharsets.UTF_8));
            if (bodyBytes.length > 0) {
                outputStream.write(bodyBytes);
            }
            outputStream.flush();
            socket.shutdownOutput();

            ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                responseBytes.write(chunk, 0, bytesRead);
            }

            return responseBytes.toString(StandardCharsets.UTF_8);
        }
    }
}
