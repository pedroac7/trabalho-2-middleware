package br.ufrn.middleware.protocol;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.error.InvocationException;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.marshaller.ResponseMarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpProtocolPlugin implements ProtocolPlugin {
    private static final int DEFAULT_MAX_PACKET_SIZE = 8192;

    private final int port;
    private final int maxPacketSize;

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
            ParsedUdpRequest parsedRequest = parseRequest(packetData);
            InvocationRequest invocationRequest = new InvocationRequest(
                    parsedRequest.method,
                    parsedRequest.path,
                    parsedRequest.queryParams,
                    parsedRequest.body
            );

            InvocationResponse response = broker.handle(invocationRequest);
            sendJson(sourceAddress, sourcePort, responseMarshaller.marshal(response));
        } catch (BadRequestException exception) {
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

    private ParsedUdpRequest parseRequest(byte[] packetData) throws BadRequestException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(packetData)) {
            Map<String, String> fields = new HashMap<>();

            while (true) {
                String line = readLine(inputStream);
                if (line == null) {
                    throw new BadRequestException("Unexpected end of request.");
                }
                if (line.isEmpty()) {
                    break;
                }

                int separatorIndex = line.indexOf(' ');
                String key;
                String value;

                if (separatorIndex < 0) {
                    key = line.trim().toUpperCase();
                    value = "";
                } else {
                    key = line.substring(0, separatorIndex).trim().toUpperCase();
                    value = line.substring(separatorIndex + 1).trim();
                }

                if (key.isEmpty()) {
                    throw new BadRequestException("Invalid request field.");
                }

                fields.put(key, value);
            }

            String method = fields.get("METHOD");
            String path = fields.get("PATH");
            String bodyLengthRaw = fields.get("BODY_LENGTH");
            String query = fields.getOrDefault("QUERY", "");

            if (method == null || method.isBlank()) {
                throw new BadRequestException("Missing METHOD field.");
            }
            if (path == null || path.isBlank()) {
                throw new BadRequestException("Missing PATH field.");
            }
            if (bodyLengthRaw == null || bodyLengthRaw.isBlank()) {
                throw new BadRequestException("Missing BODY_LENGTH field.");
            }

            int bodyLength = parseBodyLength(bodyLengthRaw);
            String body = bodyLength > 0
                    ? new String(readExactBytes(inputStream, bodyLength), StandardCharsets.UTF_8)
                    : null;

            Map<String, String> queryParams = parseQueryString(query);
            return new ParsedUdpRequest(method, path, queryParams, body);
        } catch (IOException exception) {
            throw new BadRequestException("Failed to parse UDP request.");
        }
    }

    private int parseBodyLength(String bodyLengthRaw) throws BadRequestException {
        try {
            int bodyLength = Integer.parseInt(bodyLengthRaw.trim());
            if (bodyLength < 0) {
                throw new BadRequestException("Invalid BODY_LENGTH value.");
            }
            return bodyLength;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid BODY_LENGTH value.");
        }
    }

    private byte[] readExactBytes(InputStream inputStream, int byteCount) throws IOException, BadRequestException {
        byte[] data = new byte[byteCount];
        int offset = 0;

        while (offset < byteCount) {
            int bytesRead = inputStream.read(data, offset, byteCount - offset);
            if (bytesRead == -1) {
                throw new BadRequestException("Unexpected end of request body.");
            }
            offset += bytesRead;
        }

        return data;
    }

    private String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean readAnyByte = false;

        while (true) {
            int currentByte = inputStream.read();
            if (currentByte == -1) {
                break;
            }
            readAnyByte = true;
            if (currentByte == '\n') {
                break;
            }
            if (currentByte != '\r') {
                buffer.write(currentByte);
            }
        }

        if (!readAnyByte && buffer.size() == 0) {
            return null;
        }

        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseQueryString(String query) throws BadRequestException {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            String rawKey;
            String rawValue;
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex < 0) {
                rawKey = pair;
                rawValue = "";
            } else {
                rawKey = pair.substring(0, separatorIndex);
                rawValue = pair.substring(separatorIndex + 1);
            }

            try {
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                if (key.isEmpty()) {
                    continue;
                }

                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                params.put(key, value);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Invalid query string.");
            }
        }

        if (params.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(params);
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

    private static final class ParsedUdpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> queryParams;
        private final String body;

        private ParsedUdpRequest(String method, String path, Map<String, String> queryParams, String body) {
            this.method = method;
            this.path = path;
            this.queryParams = queryParams;
            this.body = body;
        }
    }

    private static final class BadRequestException extends Exception {
        private BadRequestException(String message) {
            super(message);
        }
    }
}
