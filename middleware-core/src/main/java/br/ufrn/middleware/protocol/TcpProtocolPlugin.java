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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpProtocolPlugin implements ProtocolPlugin {
    private final int port;
    private final InvocationRequestMarshaller invocationRequestMarshaller;

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
        this.invocationRequestMarshaller = new SimpleTextInvocationRequestMarshaller();
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
                byte[] requestBytes = readRequestBytes(inputStream);
                TextInvocationMessage message = invocationRequestMarshaller.unmarshal(requestBytes);
                InvocationRequest invocationRequest = new InvocationRequest(
                        message.getRequestId(),
                        message.getMethod(),
                        message.getPath(),
                        message.getQueryParams(),
                        message.getBody()
                );

                InvocationResponse response = broker.handle(invocationRequest);
                writeJson(outputStream, responseMarshaller.marshal(response));
            } catch (MarshallingException exception) {
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

    private byte[] readRequestBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String bodyLengthRaw = null;

        while (true) {
            String line = readLine(inputStream);
            if (line == null) {
                throw new MarshallingException("Invalid request message: unexpected end of request.");
            }

            output.write(line.getBytes(StandardCharsets.UTF_8));
            output.write('\n');

            if (line.isEmpty()) {
                break;
            }

            if (line.startsWith("BODY_LENGTH")) {
                int separatorIndex = line.indexOf(' ');
                bodyLengthRaw = separatorIndex < 0 ? "" : line.substring(separatorIndex + 1).trim();
            }
        }

        if (bodyLengthRaw == null || bodyLengthRaw.isBlank()) {
            throw new MarshallingException("Invalid request message: missing BODY_LENGTH field.");
        }

        int bodyLength = parseBodyLength(bodyLengthRaw);
        byte[] bodyBytes = readExactBytes(inputStream, bodyLength);
        output.write(bodyBytes);
        return output.toByteArray();
    }

    private int parseBodyLength(String bodyLengthRaw) {
        try {
            int bodyLength = Integer.parseInt(bodyLengthRaw.trim());
            if (bodyLength < 0) {
                throw new MarshallingException("Invalid request message: BODY_LENGTH must be >= 0.");
            }
            return bodyLength;
        } catch (NumberFormatException exception) {
            throw new MarshallingException("Invalid request message: BODY_LENGTH must be an integer.", exception);
        }
    }

    private byte[] readExactBytes(InputStream inputStream, int byteCount) throws IOException {
        byte[] data = new byte[byteCount];
        int offset = 0;

        while (offset < byteCount) {
            int bytesRead = inputStream.read(data, offset, byteCount - offset);
            if (bytesRead == -1) {
                throw new MarshallingException("Invalid request message: truncated body.");
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
}
