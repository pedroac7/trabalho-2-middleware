package br.ufrn.middleware.interceptor;

import java.time.Instant;
import java.util.UUID;

public class InvocationContext {
    private final String requestId;
    private final String protocol;
    private final String httpMethod;
    private final String path;
    private final Instant startedAt;
    private Instant finishedAt;
    private Integer statusCode;
    private String componentName;
    private String remoteMethodName;
    private Throwable error;

    public InvocationContext(String protocol, String httpMethod, String path) {
        this(null, protocol, httpMethod, path);
    }

    public InvocationContext(String requestId, String protocol, String httpMethod, String path) {
        this.requestId = normalizeRequestId(requestId);
        this.protocol = protocol == null ? "unknown" : protocol;
        this.httpMethod = httpMethod == null ? "UNKNOWN" : httpMethod;
        this.path = path == null ? "/" : path;
        this.startedAt = Instant.now();
        this.finishedAt = null;
        this.statusCode = null;
        this.error = null;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getRemoteMethodName() {
        return remoteMethodName;
    }

    public Throwable getError() {
        return error;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setRemoteMethodName(String remoteMethodName) {
        this.remoteMethodName = remoteMethodName;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void markFinished(int statusCode) {
        this.statusCode = statusCode;
        this.finishedAt = Instant.now();
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}
