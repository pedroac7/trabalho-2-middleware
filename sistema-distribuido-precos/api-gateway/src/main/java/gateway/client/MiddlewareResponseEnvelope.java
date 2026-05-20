package gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class MiddlewareResponseEnvelope {
    private final boolean success;
    private final int statusCode;
    private final JsonNode result;
    private final String error;

    private MiddlewareResponseEnvelope(boolean success, int statusCode, JsonNode result, String error) {
        this.success = success;
        this.statusCode = statusCode;
        this.result = result;
        this.error = error == null ? "" : error;
    }

    static MiddlewareResponseEnvelope fromJson(ObjectMapper objectMapper, String body) throws IOException {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        if (body == null || body.isBlank()) {
            throw new IOException("RESPOSTA_VAZIA_DO_MIDDLEWARE");
        }

        JsonNode root = objectMapper.readTree(body);
        boolean success = root.path("success").asBoolean(false);
        int statusCode = root.path("statusCode").asInt(0);
        JsonNode result = root.get("result");
        String error = root.path("error").asText("");
        return new MiddlewareResponseEnvelope(success, statusCode, result, error);
    }

    boolean success() {
        return success;
    }

    int statusCode() {
        return statusCode;
    }

    JsonNode result() {
        return result;
    }

    String messageOrDefault(String defaultMessage) {
        if (!error.isBlank()) {
            return error;
        }
        if (result != null && result.isTextual()) {
            return result.asText();
        }
        if (result != null && result.hasNonNull("mensagem")) {
            return result.get("mensagem").asText();
        }
        return defaultMessage;
    }
}
