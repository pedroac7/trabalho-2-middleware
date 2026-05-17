package br.ufrn.middleware.identification;

import java.util.Objects;

public final class ObjectId {
    private final String value;

    private ObjectId(String value) {
        this.value = value;
    }

    public static ObjectId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ObjectId value must not be null.");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("ObjectId value must not be empty.");
        }

        return new ObjectId(normalized);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ObjectId objectId)) {
            return false;
        }
        return Objects.equals(value, objectId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
