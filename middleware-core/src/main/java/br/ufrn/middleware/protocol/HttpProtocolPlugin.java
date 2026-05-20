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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpProtocolPlugin implements ProtocolPlugin {
    private static final int DEFAULT_WORKER_THREADS = 20;
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;

    private final int port;
    private final int workerThreads;
    private final int queueCapacity;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;
    private Thread acceptThread;
    private Broker broker;
    private ResponseMarshaller responseMarshaller;

    public HttpProtocolPlugin(int port) {
        this(port, DEFAULT_WORKER_THREADS, DEFAULT_QUEUE_CAPACITY);
    }

    public HttpProtocolPlugin(int port, int workerThreads, int queueCapacity) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be greater than zero.");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be greater than zero.");
        }
        this.port = port;
        this.workerThreads = workerThreads;
        this.queueCapacity = queueCapacity;
    }

    @Override
    public String getName() {
        return "http";
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
            this.executor = new ThreadPoolExecutor(
                    workerThreads,
                    workerThreads,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCapacity),
                    new ThreadPoolExecutor.AbortPolicy()
            );
            this.broker = broker;
            this.responseMarshaller = responseMarshaller;
            this.running = true;
            this.acceptThread = new Thread(this::acceptLoop, "middleware-http-accept-" + port);
            this.acceptThread.start();
        } catch (IOException exception) {
            throw new RemotingException("Failed to start HTTP protocol on port " + port + ".", exception);
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
                Socket clientSocket = serverSocket.accept();
                try {
                    executor.execute(() -> handleConnection(clientSocket));
                } catch (RejectedExecutionException exception) {
                    handleBusyConnection(clientSocket);
                }
            } catch (SocketException exception) {
                if (running) {
                    throw new RemotingException("Socket error in HTTP accept loop.", exception);
                }
                return;
            } catch (IOException exception) {
                if (running) {
                    throw new RemotingException("I/O error in HTTP accept loop.", exception);
                }
                return;
            }
        }
    }

    private void handleBusyConnection(Socket socket) {
        try (Socket clientSocket = socket) {
            writeErrorResponse(clientSocket.getOutputStream(), 503, "SERVER_BUSY");
        } catch (IOException ignored) {
        }
    }

    private void handleConnection(Socket socket) {
        try (Socket clientSocket = socket) {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            try {
                ParsedHttpRequest parsedRequest = parseRequest(inputStream);
                InvocationRequest request = new InvocationRequest(
                        parsedRequest.requestId,
                        parsedRequest.httpMethod,
                        parsedRequest.path,
                        parsedRequest.queryParams,
                        parsedRequest.body
                );
                InvocationResponse response = broker.handle(request);
                String responseBody = responseMarshaller.marshal(response);
                writeHttpResponse(outputStream, response.getStatusCode(), responseBody);
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

    private ParsedHttpRequest parseRequest(InputStream inputStream) throws IOException, BadRequestException {
        String requestLine = readLine(inputStream);
        if (requestLine == null || requestLine.isBlank()) {
            throw new BadRequestException("Invalid request line.");
        }

        String[] requestLineParts = requestLine.trim().split("\\s+");
        if (requestLineParts.length < 3) {
            throw new BadRequestException("Invalid request line.");
        }

        String method = requestLineParts[0];
        String target = requestLineParts[1];
        if (method.isBlank() || target.isBlank()) {
            throw new BadRequestException("Invalid request line.");
        }

        Map<String, String> headers = readHeaders(inputStream);
        String requestId = headers.get("x-request-id");
        int contentLength = parseContentLength(headers.get("content-length"));
        String body = contentLength > 0
                ? new String(readExactBytes(inputStream, contentLength), StandardCharsets.UTF_8)
                : null;

        String path = target;
        String query = null;
        int queryStartIndex = target.indexOf('?');
        if (queryStartIndex >= 0) {
            path = target.substring(0, queryStartIndex);
            query = queryStartIndex + 1 < target.length() ? target.substring(queryStartIndex + 1) : "";
        }

        return new ParsedHttpRequest(requestId, method, path, parseQueryString(query), body);
    }

    private Map<String, String> readHeaders(InputStream inputStream) throws IOException, BadRequestException {
        Map<String, String> headers = new HashMap<>();

        while (true) {
            String line = readLine(inputStream);
            if (line == null) {
                throw new BadRequestException("Unexpected end of request headers.");
            }
            if (line.isEmpty()) {
                break;
            }

            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                throw new BadRequestException("Invalid header line.");
            }

            String headerName = line.substring(0, separatorIndex).trim().toLowerCase();
            String headerValue = line.substring(separatorIndex + 1).trim();
            headers.put(headerName, headerValue);
        }

        return headers;
    }

    private int parseContentLength(String contentLengthHeader) throws BadRequestException {
        if (contentLengthHeader == null || contentLengthHeader.isBlank()) {
            return 0;
        }

        try {
            int contentLength = Integer.parseInt(contentLengthHeader.trim());
            if (contentLength < 0) {
                throw new BadRequestException("Invalid Content-Length header.");
            }
            return contentLength;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid Content-Length header.");
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
        String json = responseMarshaller.marshal(response);
        writeHttpResponse(outputStream, statusCode, json);
    }

    private void writeHttpResponse(OutputStream outputStream, int statusCode, String body) throws IOException {
        String responseBody = body == null ? "" : body;
        byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);

        String header = "HTTP/1.1 "
                + statusCode
                + " "
                + reasonPhraseForStatus(statusCode)
                + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: "
                + bodyBytes.length
                + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        outputStream.write(header.getBytes(StandardCharsets.UTF_8));
        outputStream.write(bodyBytes);
        outputStream.flush();
    }

    private String reasonPhraseForStatus(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 503 -> "Service Unavailable";
            case 500 -> "Internal Server Error";
            default -> "Status";
        };
    }

    private static final class ParsedHttpRequest {
        private final String requestId;
        private final String httpMethod;
        private final String path;
        private final Map<String, String> queryParams;
        private final String body;

        private ParsedHttpRequest(
                String requestId,
                String httpMethod,
                String path,
                Map<String, String> queryParams,
                String body
        ) {
            this.requestId = requestId;
            this.httpMethod = httpMethod;
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
