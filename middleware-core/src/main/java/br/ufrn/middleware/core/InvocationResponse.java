package br.ufrn.middleware.core;

public final class InvocationResponse {
    private final int statusCode;
    private final Object body;
    private final String errorMessage;

    private InvocationResponse(int statusCode, Object body, String errorMessage) {
        this.statusCode = statusCode;
        this.body = body;
        this.errorMessage = errorMessage;
    }

    public static InvocationResponse success(Object body) {
        return new InvocationResponse(200, body, null);
    }

    public static InvocationResponse error(int statusCode, String errorMessage) {
        if (statusCode < 400) {
            throw new IllegalArgumentException("Error status code must be greater than or equal to 400.");
        }
        return new InvocationResponse(statusCode, null, errorMessage);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getBody() {
        return body;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
