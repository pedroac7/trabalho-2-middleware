package br.ufrn.middleware.identification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class AbsoluteObjectReference {
    private final String protocol;
    private final String host;
    private final int port;
    private final ObjectId objectId;

    public AbsoluteObjectReference(String protocol, String host, int port, ObjectId objectId) {
        if (protocol == null || protocol.trim().isEmpty()) {
            throw new IllegalArgumentException("Protocol must not be null or empty.");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host must not be null or empty.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        if (objectId == null) {
            throw new IllegalArgumentException("ObjectId must not be null.");
        }

        this.protocol = protocol.trim();
        this.host = host.trim();
        this.port = port;
        this.objectId = objectId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public String toPath() {
        return "/" + objectId.getValue();
    }

    public URI toUri() {
        try {
            return new URI(protocol, null, host, port, toPath(), null, null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Could not build URI for absolute object reference.", exception);
        }
    }

    public static AbsoluteObjectReference fromUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("URI must not be null or empty.");
        }

        final URI parsedUri;
        try {
            parsedUri = new URI(uri.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URI: " + uri, exception);
        }

        String protocol = parsedUri.getScheme();
        String host = parsedUri.getHost();
        int port = parsedUri.getPort();
        String path = parsedUri.getPath();

        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("URI must contain a protocol.");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URI must contain a host.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("URI must contain a valid port between 1 and 65535.");
        }
        if (path == null || path.isBlank() || "/".equals(path)) {
            throw new IllegalArgumentException("URI must contain an object id path.");
        }

        String objectIdValue = path.startsWith("/") ? path.substring(1) : path;
        ObjectId objectId = ObjectId.of(objectIdValue);
        return new AbsoluteObjectReference(protocol, host, port, objectId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbsoluteObjectReference that)) {
            return false;
        }
        return port == that.port
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(host, that.host)
                && Objects.equals(objectId, that.objectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, host, port, objectId);
    }

    @Override
    public String toString() {
        return toUri().toString();
    }
}
