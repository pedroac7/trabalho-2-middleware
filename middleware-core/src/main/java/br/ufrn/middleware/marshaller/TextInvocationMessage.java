package br.ufrn.middleware.marshaller;

import java.util.Map;
import java.util.UUID;

public final class TextInvocationMessage {
    private final String requestId;
    private final String method;
    private final String path;
    private final Map<String, String> queryParams;
    private final String body;

    public TextInvocationMessage(String method, String path, Map<String, String> queryParams, String body) {
        this(null, method, path, queryParams, body);
    }

    public TextInvocationMessage(
            String requestId,
            String method,
            String path,
            Map<String, String> queryParams,
            String body
    ) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Method must not be null or blank.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or blank.");
        }

        this.requestId = normalizeRequestId(requestId);
        this.method = method.trim().toUpperCase();
        this.path = normalizePath(path);
        this.queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        this.body = body == null ? "" : body;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getBody() {
        return body;
    }

    private String normalizePath(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}
