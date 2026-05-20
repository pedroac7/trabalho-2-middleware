package br.ufrn.middleware.client;

import br.ufrn.middleware.identification.AbsoluteObjectReference;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientRequestHandlerTimeoutTest {

    @Test
    void httpHandlerReturns504WhenRequestTimesOut() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            Thread serverThread = new Thread(() -> holdHttpConnection(serverSocket, 800), "http-timeout-server");
            serverThread.setDaemon(true);
            serverThread.start();

            HttpClientRequestHandler handler = new HttpClientRequestHandler(100);
            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    AbsoluteObjectReference.fromUri("http://localhost:" + port + "/gateway"),
                    "POST",
                    "/precos",
                    Map.of(),
                    "{\"ativo\":\"PETR4\",\"valor\":35.5,\"timestamp\":1710000000000}"
            );

            RemoteInvocationResponse response = handler.send(request);
            assertEquals(504, response.getStatusCode());
            assertTrue(response.getBody().contains("REQUEST_TIMEOUT"));
        }
    }

    @Test
    void tcpHandlerReturns504WhenRequestTimesOut() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            Thread serverThread = new Thread(() -> holdTcpConnection(serverSocket, 800), "tcp-timeout-server");
            serverThread.setDaemon(true);
            serverThread.start();

            TcpClientRequestHandler handler = new TcpClientRequestHandler(100);
            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    AbsoluteObjectReference.fromUri("tcp://localhost:" + port + "/gateway"),
                    "POST",
                    "/precos",
                    Map.of(),
                    "{\"ativo\":\"PETR4\",\"valor\":35.5,\"timestamp\":1710000000000}"
            );

            RemoteInvocationResponse response = handler.send(request);
            assertEquals(504, response.getStatusCode());
            assertTrue(response.getBody().contains("REQUEST_TIMEOUT"));
        }
    }

    @Test
    void udpHandlerReturns504WhenRequestTimesOut() throws Exception {
        try (DatagramSocket datagramSocket = new DatagramSocket(0)) {
            int port = datagramSocket.getLocalPort();
            Thread serverThread = new Thread(() -> holdUdpPacket(datagramSocket, 800), "udp-timeout-server");
            serverThread.setDaemon(true);
            serverThread.start();

            UdpClientRequestHandler handler = new UdpClientRequestHandler(100);
            RemoteInvocationRequest request = new RemoteInvocationRequest(
                    AbsoluteObjectReference.fromUri("udp://localhost:" + port + "/gateway"),
                    "POST",
                    "/precos",
                    Map.of(),
                    "{\"ativo\":\"PETR4\",\"valor\":35.5,\"timestamp\":1710000000000}"
            );

            RemoteInvocationResponse response = handler.send(request);
            assertEquals(504, response.getStatusCode());
            assertTrue(response.getBody().contains("REQUEST_TIMEOUT"));
        }
    }

    private void holdHttpConnection(ServerSocket serverSocket, long holdMillis) {
        try (Socket socket = serverSocket.accept()) {
            Thread.sleep(holdMillis);
        } catch (Exception ignored) {
        }
    }

    private void holdTcpConnection(ServerSocket serverSocket, long holdMillis) {
        try (Socket socket = serverSocket.accept()) {
            Thread.sleep(holdMillis);
        } catch (Exception ignored) {
        }
    }

    private void holdUdpPacket(DatagramSocket socket, long holdMillis) {
        try {
            byte[] buffer = new byte[8192];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            Thread.sleep(holdMillis);
        } catch (Exception ignored) {
        }
    }
}
