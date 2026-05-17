package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.core.InvocationResponse;

public class SimpleJsonResponseMarshaller implements ResponseMarshaller {
    @Override
    public String marshal(InvocationResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("InvocationResponse must not be null.");
        }

        try {
            if (response.getStatusCode() >= 400) {
                return "{\"success\":false,\"statusCode\":"
                        + response.getStatusCode()
                        + ",\"error\":"
                        + serializeAsStringOrNull(response.getErrorMessage())
                        + "}";
            }

            return "{\"success\":true,\"statusCode\":"
                    + response.getStatusCode()
                    + ",\"result\":"
                    + serializeResult(response.getBody())
                    + "}";
        } catch (RuntimeException exception) {
            throw new MarshallingException("Failed to marshal invocation response.", exception);
        }
    }

    private String serializeResult(Object body) {
        if (body == null) {
            return "null";
        }

        if (body instanceof Number || body instanceof Boolean) {
            return body.toString();
        }

        if (body instanceof Character || body instanceof String) {
            return quote(escape(body.toString()));
        }

        return quote(escape(body.toString()));
    }

    private String serializeAsStringOrNull(String value) {
        if (value == null) {
            return "null";
        }
        return quote(escape(value));
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
