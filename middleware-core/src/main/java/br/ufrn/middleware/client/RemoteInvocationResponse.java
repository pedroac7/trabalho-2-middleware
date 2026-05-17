package br.ufrn.middleware.client;

public final class RemoteInvocationResponse {
    private final int statusCode;
    private final String body;

    public RemoteInvocationResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body == null ? "" : body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
