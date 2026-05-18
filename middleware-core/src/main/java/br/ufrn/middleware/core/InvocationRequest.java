package br.ufrn.middleware.core;

import java.util.Map;
import java.util.UUID;

public final class InvocationRequest {
    private final String requestId;
    private final String httpMethod;
    private final String path;
    private final Map<String, String> queryParams;
    private final String body;

    public InvocationRequest(String httpMethod, String path, Map<String, String> queryParams, String body) {
        this(null, httpMethod, path, queryParams, body);
    }

    public InvocationRequest(
            String requestId,
            String httpMethod,
            String path,
            Map<String, String> queryParams,
            String body
    ) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or blank.");
        }

        this.requestId = normalizeRequestId(requestId);
        this.httpMethod = httpMethod.trim().toUpperCase();
        this.path = normalizePath(path);
        this.queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        this.body = body;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getHttpMethod() {
        return httpMethod;
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

    private static String normalizePath(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}
