package br.ufrn.middleware.client;

import br.ufrn.middleware.identification.AbsoluteObjectReference;

import java.util.Map;

public final class RemoteInvocationRequest {
    private final AbsoluteObjectReference objectReference;
    private final String httpMethod;
    private final String remotePath;
    private final Map<String, String> queryParams;
    private final String body;

    public RemoteInvocationRequest(
            AbsoluteObjectReference objectReference,
            String httpMethod,
            String remotePath,
            Map<String, String> queryParams,
            String body
    ) {
        if (objectReference == null) {
            throw new IllegalArgumentException("Object reference must not be null.");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be null or blank.");
        }
        if (remotePath == null || remotePath.isBlank()) {
            throw new IllegalArgumentException("Remote path must not be null or blank.");
        }

        this.objectReference = objectReference;
        this.httpMethod = httpMethod.trim().toUpperCase();
        this.remotePath = normalizeRemotePath(remotePath);
        this.queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
        this.body = body;
    }

    public AbsoluteObjectReference getObjectReference() {
        return objectReference;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getBody() {
        return body;
    }

    private static String normalizeRemotePath(String remotePath) {
        String normalized = remotePath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
