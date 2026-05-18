package br.ufrn.middleware.protocol;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.error.InvocationException;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.marshaller.InvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.MarshallingException;
import br.ufrn.middleware.marshaller.ResponseMarshaller;
import br.ufrn.middleware.marshaller.SimpleTextInvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.TextInvocationMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpProtocolPlugin implements ProtocolPlugin {
    private static final int DEFAULT_MAX_PACKET_SIZE = 8192;

    private final int port;
    private final int maxPacketSize;
    private final InvocationRequestMarshaller invocationRequestMarshaller;

    private DatagramSocket socket;
    private ExecutorService executor;
    private volatile boolean running;
    private Thread receiveThread;
    private Broker broker;
    private ResponseMarshaller responseMarshaller;

    public UdpProtocolPlugin(int port) {
        this(port, DEFAULT_MAX_PACKET_SIZE);
    }

    public UdpProtocolPlugin(int port, int maxPacketSize) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        if (maxPacketSize <= 0) {
            throw new IllegalArgumentException("maxPacketSize must be greater than zero.");
        }
        this.port = port;
        this.maxPacketSize = maxPacketSize;
        this.invocationRequestMarshaller = new SimpleTextInvocationRequestMarshaller();
    }

    @Override
    public String getName() {
        return "udp";
    }

    @Override
    public synchronized void start(Broker broker, ResponseMarshaller responseMarshaller) {
        if (broker == null) {
            throw new IllegalArgumentException("Broker must not be null.");
        }
        if (responseMarshaller == null) {
            throw new IllegalArgumentException("ResponseMarshaller must not be null.");
        }
        if (running) {
            return;
        }

        try {
            this.socket = new DatagramSocket(port);
            this.executor = Executors.newFixedThreadPool(20);
            this.broker = broker;
            this.responseMarshaller = responseMarshaller;
            this.running = true;
            this.receiveThread = new Thread(this::receiveLoop, "middleware-udp-receive-" + port);
            this.receiveThread.start();
        } catch (IOException exception) {
            throw new RemotingException("Failed to start UDP protocol on port " + port + ".", exception);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        if (socket != null) {
            socket.close();
            socket = null;
        }

        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void receiveLoop() {
        while (running) {
            try {
                byte[] buffer = new byte[maxPacketSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] packetData = Arrays.copyOfRange(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getOffset() + packet.getLength()
                );
                java.net.InetAddress sourceAddress = packet.getAddress();
                int sourcePort = packet.getPort();

                executor.submit(() -> handlePacket(packetData, sourceAddress, sourcePort));
            } catch (SocketException exception) {
                if (running) {
                    throw new RemotingException("Socket error in UDP receive loop.", exception);
                }
                return;
            } catch (IOException exception) {
                if (running) {
                    throw new RemotingException("I/O error in UDP receive loop.", exception);
                }
                return;
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void handlePacket(byte[] packetData, java.net.InetAddress sourceAddress, int sourcePort) {
        try {
            TextInvocationMessage message = invocationRequestMarshaller.unmarshal(packetData);
            InvocationRequest invocationRequest = new InvocationRequest(
                    message.getMethod(),
                    message.getPath(),
                    message.getQueryParams(),
                    message.getBody()
            );

            InvocationResponse response = broker.handle(invocationRequest);
            sendJson(sourceAddress, sourcePort, responseMarshaller.marshal(response));
        } catch (MarshallingException exception) {
            sendErrorResponse(sourceAddress, sourcePort, 400, exception.getMessage());
        } catch (RemoteMethodNotFoundException exception) {
            sendErrorResponse(sourceAddress, sourcePort, 404, exception.getMessage());
        } catch (BindingException exception) {
            sendErrorResponse(sourceAddress, sourcePort, 400, exception.getMessage());
        } catch (InvocationException exception) {
            sendErrorResponse(sourceAddress, sourcePort, 500, exception.getMessage());
        } catch (RemotingException exception) {
            sendErrorResponse(sourceAddress, sourcePort, 500, exception.getMessage());
        } catch (Exception exception) {
            sendErrorResponse(sourceAddress, sourcePort, 500, "Unexpected server error.");
        }
    }

    private void sendErrorResponse(java.net.InetAddress address, int port, int statusCode, String message) {
        String errorMessage = message == null || message.isBlank() ? reasonPhraseForStatus(statusCode) : message;
        InvocationResponse response = InvocationResponse.error(statusCode, errorMessage);
        sendJson(address, port, responseMarshaller.marshal(response));
    }

    private void sendJson(java.net.InetAddress address, int port, String json) {
        try {
            byte[] responseBytes = ensurePacketSize(json);
            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes,
                    responseBytes.length,
                    address,
                    port
            );
            synchronized (this) {
                if (socket != null && !socket.isClosed()) {
                    socket.send(responsePacket);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private byte[] ensurePacketSize(String json) {
        byte[] responseBytes = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);
        if (responseBytes.length <= maxPacketSize) {
            return responseBytes;
        }

        String overflowJson = responseMarshaller.marshal(
                InvocationResponse.error(500, "Response too large for UDP packet")
        );
        byte[] overflowBytes = overflowJson.getBytes(StandardCharsets.UTF_8);
        if (overflowBytes.length <= maxPacketSize) {
            return overflowBytes;
        }

        String minimalOverflow = "{\"success\":false,\"statusCode\":500,\"error\":\"Response too large for UDP packet\"}";
        byte[] minimalBytes = minimalOverflow.getBytes(StandardCharsets.UTF_8);
        if (minimalBytes.length <= maxPacketSize) {
            return minimalBytes;
        }

        return Arrays.copyOf(minimalBytes, maxPacketSize);
    }

    private String reasonPhraseForStatus(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Status";
        };
    }
}
