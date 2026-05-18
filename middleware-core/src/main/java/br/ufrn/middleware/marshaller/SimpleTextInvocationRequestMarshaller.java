package br.ufrn.middleware.marshaller;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SimpleTextInvocationRequestMarshaller implements InvocationRequestMarshaller {
    @Override
    public byte[] marshal(TextInvocationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("TextInvocationMessage must not be null.");
        }

        try {
            String query = buildQueryString(message.getQueryParams());
            byte[] bodyBytes = message.getBody().getBytes(StandardCharsets.UTF_8);

            StringBuilder builder = new StringBuilder();
            builder.append("REQUEST_ID ").append(message.getRequestId()).append('\n');
            builder.append("METHOD ").append(message.getMethod()).append('\n');
            builder.append("PATH ").append(message.getPath()).append('\n');
            builder.append("QUERY");
            if (!query.isEmpty()) {
                builder.append(' ').append(query);
            }
            builder.append('\n');
            builder.append("BODY_LENGTH ").append(bodyBytes.length).append('\n');
            builder.append('\n');

            ByteArrayOutputStream output = new ByteArrayOutputStream(builder.length() + bodyBytes.length);
            output.write(builder.toString().getBytes(StandardCharsets.UTF_8));
            output.write(bodyBytes);
            return output.toByteArray();
        } catch (RuntimeException exception) {
            throw new MarshallingException("Failed to marshal invocation message.", exception);
        } catch (Exception exception) {
            throw new MarshallingException("Failed to marshal invocation message.", exception);
        }
    }

    @Override
    public TextInvocationMessage unmarshal(byte[] bytes) {
        if (bytes == null) {
            throw new MarshallingException("Request message bytes must not be null.");
        }

        try {
            int index = 0;
            Map<String, String> fields = new HashMap<>();
            boolean separatorFound = false;

            while (index < bytes.length) {
                int lineEnd = findLineEnd(bytes, index);
                if (lineEnd < 0) {
                    throw new MarshallingException("Invalid request message: missing header/body separator line.");
                }

                String line = decodeLine(bytes, index, lineEnd);
                index = lineEnd + 1;

                if (line.isEmpty()) {
                    separatorFound = true;
                    break;
                }

                int separatorIndex = line.indexOf(' ');
                String key;
                String value;

                if (separatorIndex < 0) {
                    key = line.trim().toUpperCase();
                    value = "";
                } else {
                    key = line.substring(0, separatorIndex).trim().toUpperCase();
                    value = line.substring(separatorIndex + 1).trim();
                }

                if (key.isEmpty()) {
                    throw new MarshallingException("Invalid request message: empty header name.");
                }

                fields.put(key, value);
            }

            if (!separatorFound) {
                throw new MarshallingException("Invalid request message: missing blank line before body.");
            }

            String method = fields.get("METHOD");
            String path = fields.get("PATH");
            String bodyLengthRaw = fields.get("BODY_LENGTH");
            String requestId = fields.get("REQUEST_ID");
            String query = fields.getOrDefault("QUERY", "");

            if (method == null || method.isBlank()) {
                throw new MarshallingException("Invalid request message: missing METHOD field.");
            }
            if (path == null || path.isBlank()) {
                throw new MarshallingException("Invalid request message: missing PATH field.");
            }
            if (bodyLengthRaw == null || bodyLengthRaw.isBlank()) {
                throw new MarshallingException("Invalid request message: missing BODY_LENGTH field.");
            }

            int bodyLength = parseBodyLength(bodyLengthRaw);
            int availableBodyBytes = bytes.length - index;
            if (availableBodyBytes < bodyLength) {
                throw new MarshallingException("Invalid request message: truncated body.");
            }

            String body = new String(bytes, index, bodyLength, StandardCharsets.UTF_8);
            Map<String, String> queryParams = parseQueryString(query);
            return new TextInvocationMessage(requestId, method, path, queryParams, body);
        } catch (MarshallingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MarshallingException("Failed to unmarshal invocation message.", exception);
        }
    }

    private int findLineEnd(byte[] bytes, int startIndex) {
        for (int i = startIndex; i < bytes.length; i++) {
            if (bytes[i] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private String decodeLine(byte[] bytes, int startIndex, int lineEndIndex) {
        int effectiveEnd = lineEndIndex;
        if (effectiveEnd > startIndex && bytes[effectiveEnd - 1] == '\r') {
            effectiveEnd--;
        }
        return new String(bytes, startIndex, effectiveEnd - startIndex, StandardCharsets.UTF_8);
    }

    private int parseBodyLength(String bodyLengthRaw) {
        try {
            int bodyLength = Integer.parseInt(bodyLengthRaw.trim());
            if (bodyLength < 0) {
                throw new MarshallingException("Invalid request message: BODY_LENGTH must be >= 0.");
            }
            return bodyLength;
        } catch (NumberFormatException exception) {
            throw new MarshallingException("Invalid request message: BODY_LENGTH must be an integer.", exception);
        }
    }

    private String buildQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }

            String value = entry.getValue() == null ? "" : entry.getValue();
            queryBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            queryBuilder.append('=');
            queryBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }

        return queryBuilder.toString();
    }

    private Map<String, String> parseQueryString(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            int separatorIndex = pair.indexOf('=');
            String rawKey = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
            String rawValue = separatorIndex < 0 ? "" : pair.substring(separatorIndex + 1);

            try {
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                if (key.isEmpty()) {
                    continue;
                }

                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                params.put(key, value);
            } catch (IllegalArgumentException exception) {
                throw new MarshallingException("Invalid request message: query string is malformed.", exception);
            }
        }

        if (params.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(params);
    }
}
