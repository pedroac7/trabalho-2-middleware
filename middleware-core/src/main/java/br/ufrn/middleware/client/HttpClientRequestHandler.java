package br.ufrn.middleware.client;

import br.ufrn.middleware.error.RemotingException;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpClientRequestHandler implements ClientRequestHandler {
    private final HttpClient httpClient;

    public HttpClientRequestHandler() {
        this(HttpClient.newHttpClient());
    }

    public HttpClientRequestHandler(HttpClient httpClient) {
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient must not be null.");
        }
        this.httpClient = httpClient;
    }

    @Override
    public RemoteInvocationResponse send(RemoteInvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RemoteInvocationRequest must not be null.");
        }

        URI uri = buildFinalUri(request);
        HttpRequest httpRequest = buildHttpRequest(request, uri);

        try {
            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RemoteInvocationResponse(response.statusCode(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RemotingException("HTTP request was interrupted.", exception);
        } catch (IOException exception) {
            throw new RemotingException("Failed to send HTTP request.", exception);
        }
    }

    private HttpRequest buildHttpRequest(RemoteInvocationRequest request, URI uri) {
        String method = request.getHttpMethod();
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);

        if ("GET".equals(method)) {
            return builder.GET().build();
        }

        if ("POST".equals(method)) {
            String body = request.getBody() == null ? "" : request.getBody();
            return builder
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
        }

        String body = request.getBody() == null ? "" : request.getBody();
        return builder
                .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    private URI buildFinalUri(RemoteInvocationRequest request) {
        AbsoluteObjectReference reference = request.getObjectReference();
        String path = combinePath(reference.toPath(), request.getRemotePath());
        String query = buildQueryString(request.getQueryParams());

        try {
            return new URI(
                    reference.getProtocol(),
                    null,
                    reference.getHost(),
                    reference.getPort(),
                    path,
                    query.isEmpty() ? null : query,
                    null
            );
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid request URI.", exception);
        }
    }

    private String combinePath(String basePath, String remotePath) {
        String left = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String right = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
        return (left + right).replaceAll("/+", "/");
    }

    private String buildQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            if (queryString.length() > 0) {
                queryString.append('&');
            }

            String value = entry.getValue() == null ? "" : entry.getValue();
            queryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            queryString.append('=');
            queryString.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }

        return queryString.toString();
    }
}
