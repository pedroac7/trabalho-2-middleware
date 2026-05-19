package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.core.InvocationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleJsonResponseMarshaller implements ResponseMarshaller {
    private final ObjectMapper objectMapper;

    public SimpleJsonResponseMarshaller() {
        this(new ObjectMapper());
    }

    public SimpleJsonResponseMarshaller(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        this.objectMapper = objectMapper;
    }

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

        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new MarshallingException("Failed to serialize response object body.", exception);
        }
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
