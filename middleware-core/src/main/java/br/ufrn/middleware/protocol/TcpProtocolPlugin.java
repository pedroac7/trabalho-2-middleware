package br.ufrn.middleware.protocol;

import br.ufrn.middleware.core.Broker;
import br.ufrn.middleware.core.InvocationRequest;
import br.ufrn.middleware.core.InvocationResponse;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.error.InvocationException;
import br.ufrn.middleware.error.RemoteMethodNotFoundException;
import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.marshaller.ResponseMarshaller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpProtocolPlugin implements ProtocolPlugin {
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;
    private Thread acceptThread;
    private Broker broker;
    private ResponseMarshaller responseMarshaller;

    public TcpProtocolPlugin(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        this.port = port;
    }

    @Override
    public String getName() {
        return "tcp";
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
            this.serverSocket = new ServerSocket(port);
            this.executor = Executors.newFixedThreadPool(20);
            this.broker = broker;
            this.responseMarshaller = responseMarshaller;
            this.running = true;
            this.acceptThread = new Thread(this::acceptLoop, "middleware-tcp-accept-" + port);
            this.acceptThread.start();
        } catch (IOException exception) {
            throw new RemotingException("Failed to start TCP protocol on port " + port + ".", exception);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            } finally {
                serverSocket = null;
            }
        }

        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleConnection(socket));
            } catch (SocketException exception) {
                if (running) {
                    throw new RemotingException("Socket error in TCP accept loop.", exception);
                }
                return;
            } catch (IOException exception) {
                if (running) {
                    throw new RemotingException("I/O error in TCP accept loop.", exception);
                }
                return;
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (Socket clientSocket = socket) {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            try {
                ParsedTcpRequest parsedRequest = parseRequest(inputStream);
                InvocationRequest invocationRequest = new InvocationRequest(
                        parsedRequest.method,
                        parsedRequest.path,
                        parsedRequest.queryParams,
                        parsedRequest.body
                );

                InvocationResponse response = broker.handle(invocationRequest);
                writeJson(outputStream, responseMarshaller.marshal(response));
            } catch (BadRequestException exception) {
                writeErrorResponse(outputStream, 400, exception.getMessage());
            } catch (RemoteMethodNotFoundException exception) {
                writeErrorResponse(outputStream, 404, exception.getMessage());
            } catch (BindingException exception) {
                writeErrorResponse(outputStream, 400, exception.getMessage());
            } catch (InvocationException exception) {
                writeErrorResponse(outputStream, 500, exception.getMessage());
            } catch (RemotingException exception) {
                writeErrorResponse(outputStream, 500, exception.getMessage());
            } catch (Exception exception) {
                writeErrorResponse(outputStream, 500, "Unexpected server error.");
            }
        } catch (IOException ignored) {
        }
    }

    private ParsedTcpRequest parseRequest(InputStream inputStream) throws IOException, BadRequestException {
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
        return new ParsedTcpRequest(method, path, queryParams, body);
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

    private void writeErrorResponse(OutputStream outputStream, int statusCode, String message) throws IOException {
        String errorMessage = message == null || message.isBlank() ? reasonPhraseForStatus(statusCode) : message;
        InvocationResponse response = InvocationResponse.error(statusCode, errorMessage);
        writeJson(outputStream, responseMarshaller.marshal(response));
    }

    private void writeJson(OutputStream outputStream, String json) throws IOException {
        byte[] jsonBytes = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);
        outputStream.write(jsonBytes);
        outputStream.flush();
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

    private static final class ParsedTcpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> queryParams;
        private final String body;

        private ParsedTcpRequest(String method, String path, Map<String, String> queryParams, String body) {
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
