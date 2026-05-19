package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.error.BindingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonBodyMarshaller {
    private final ObjectMapper objectMapper;

    public JsonBodyMarshaller() {
        this(new ObjectMapper());
    }

    public JsonBodyMarshaller(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        this.objectMapper = objectMapper;
    }

    public Object unmarshalBody(String body, Class<?> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null.");
        }

        if (targetType == String.class) {
            return body;
        }

        if (body == null || body.isBlank()) {
            throw new BindingException(
                    "Request body is required for type " + targetType.getName() + "."
            );
        }

        try {
            return objectMapper.readValue(body, targetType);
        } catch (IOException exception) {
            if (exception instanceof JsonProcessingException) {
                throw new BindingException(
                        "Invalid JSON body for type " + targetType.getName() + ".",
                        exception
                );
            }
            throw new BindingException(
                    "Failed to read JSON body for type " + targetType.getName() + ".",
                    exception
            );
        }
    }
}
