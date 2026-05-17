package br.ufrn.middleware.core;

import java.util.Map;

public final class InvocationRequest {
    private final String httpMethod;
    private final String path;
    private final Map<String, String> queryParams;
    private final String body;

    public InvocationRequest(String httpMethod, String path, Map<String, String> queryParams, String body) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be null or blank.");
        }

        this.httpMethod = httpMethod.trim().toUpperCase();
        this.path = normalizePath(path);
        this.queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        this.body = body;
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
}
