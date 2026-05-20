package br.ufrn.middleware.client;

import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.marshaller.InvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.SimpleTextInvocationRequestMarshaller;
import br.ufrn.middleware.marshaller.TextInvocationMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcpClientRequestHandler implements ClientRequestHandler {
    private static final String REQUEST_TIMEOUT_BODY = "{\"success\":false,\"statusCode\":504,\"error\":\"REQUEST_TIMEOUT\"}";
    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("\"statusCode\"\\s*:\\s*(\\d+)");
    private final InvocationRequestMarshaller invocationRequestMarshaller;
    private final int timeoutMillis;

    public TcpClientRequestHandler() {
        this(0, new SimpleTextInvocationRequestMarshaller());
    }

    public TcpClientRequestHandler(int timeoutMillis) {
        this(timeoutMillis, new SimpleTextInvocationRequestMarshaller());
    }

    public TcpClientRequestHandler(InvocationRequestMarshaller invocationRequestMarshaller) {
        this(0, invocationRequestMarshaller);
    }

    public TcpClientRequestHandler(int timeoutMillis, InvocationRequestMarshaller invocationRequestMarshaller) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("timeoutMillis must not be negative.");
        }
        if (invocationRequestMarshaller == null) {
            throw new IllegalArgumentException("InvocationRequestMarshaller must not be null.");
        }
        this.timeoutMillis = timeoutMillis;
        this.invocationRequestMarshaller = invocationRequestMarshaller;
    }

    @Override
    public RemoteInvocationResponse send(RemoteInvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RemoteInvocationRequest must not be null.");
        }

        AbsoluteObjectReference reference = request.getObjectReference();
        String protocol = reference.getProtocol();
        if (!"tcp".equalsIgnoreCase(protocol)) {
            throw new RemotingException("TcpClientRequestHandler only supports protocol tcp.");
        }

        TextInvocationMessage message = new TextInvocationMessage(
                request.getRequestId(),
                request.getHttpMethod(),
                buildPath(reference, request.getRemotePath()),
                request.getQueryParams(),
                request.getBody()
        );
        byte[] requestBytes = invocationRequestMarshaller.marshal(message);

        try (Socket socket = new Socket()) {
            InetSocketAddress endpoint = new InetSocketAddress(reference.getHost(), reference.getPort());
            if (timeoutMillis > 0) {
                socket.connect(endpoint, timeoutMillis);
                socket.setSoTimeout(timeoutMillis);
            } else {
                socket.connect(endpoint);
            }

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            outputStream.write(requestBytes);
            outputStream.flush();
            socket.shutdownOutput();

            String responseBody = readAll(inputStream);
            int statusCode = inferStatusCode(responseBody);
            return new RemoteInvocationResponse(statusCode, responseBody);
        } catch (SocketTimeoutException exception) {
            return timeoutResponse();
        } catch (IOException exception) {
            throw new RemotingException("Failed to send TCP request.", exception);
        }
    }

    private String buildPath(AbsoluteObjectReference reference, String remotePath) {
        String objectIdPath = "/" + reference.getObjectId().getValue();
        String methodPath = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
        return (objectIdPath + methodPath).replaceAll("/+", "/");
    }

    private String readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        return output.toString(StandardCharsets.UTF_8);
    }

    private int inferStatusCode(String responseBody) {
        Matcher matcher = STATUS_CODE_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        if (responseBody.contains("\"success\":true")) {
            return 200;
        }

        if (responseBody.contains("\"success\":false")) {
            return 500;
        }

        return 500;
    }

    int getTimeoutMillis() {
        return timeoutMillis;
    }

    private RemoteInvocationResponse timeoutResponse() {
        return new RemoteInvocationResponse(504, REQUEST_TIMEOUT_BODY);
    }
}
