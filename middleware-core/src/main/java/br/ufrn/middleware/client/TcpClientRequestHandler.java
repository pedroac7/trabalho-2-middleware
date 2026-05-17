package br.ufrn.middleware.client;

import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcpClientRequestHandler implements ClientRequestHandler {
    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("\"statusCode\"\\s*:\\s*(\\d+)");

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

        String method = request.getHttpMethod();
        String path = buildPath(reference, request.getRemotePath());
        String query = buildQueryString(request.getQueryParams());
        String body = request.getBody() == null ? "" : request.getBody();
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("METHOD ").append(method).append('\n');
        requestBuilder.append("PATH ").append(path).append('\n');
        requestBuilder.append("QUERY");
        if (!query.isEmpty()) {
            requestBuilder.append(' ').append(query);
        }
        requestBuilder.append('\n');
        requestBuilder.append("BODY_LENGTH ").append(bodyBytes.length).append('\n');
        requestBuilder.append('\n');

        try (Socket socket = new Socket(reference.getHost(), reference.getPort())) {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            outputStream.write(requestBuilder.toString().getBytes(StandardCharsets.UTF_8));
            if (bodyBytes.length > 0) {
                outputStream.write(bodyBytes);
            }
            outputStream.flush();
            socket.shutdownOutput();

            String responseBody = readAll(inputStream);
            int statusCode = inferStatusCode(responseBody);
            return new RemoteInvocationResponse(statusCode, responseBody);
        } catch (IOException exception) {
            throw new RemotingException("Failed to send TCP request.", exception);
        }
    }

    private String buildPath(AbsoluteObjectReference reference, String remotePath) {
        String objectIdPath = "/" + reference.getObjectId().getValue();
        String methodPath = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
        return (objectIdPath + methodPath).replaceAll("/+", "/");
    }

    private String buildQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }

            String value = entry.getValue() == null ? "" : entry.getValue();
            queryBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            queryBuilder.append('=');
            queryBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }

        return queryBuilder.toString();
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
}
