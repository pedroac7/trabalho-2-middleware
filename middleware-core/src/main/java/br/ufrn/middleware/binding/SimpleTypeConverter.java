package br.ufrn.middleware.binding;

import br.ufrn.middleware.error.BindingException;

public class SimpleTypeConverter {
    public Object convert(String value, Class<?> targetType) {
        if (targetType == null) {
            throw new BindingException("Target type must not be null.");
        }

        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new BindingException("Cannot bind null to primitive type " + targetType.getName() + ".");
            }
            return null;
        }

        if (targetType == String.class) {
            return value;
        }

        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            }
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            }
            if (targetType == char.class || targetType == Character.class) {
                if (value.length() != 1) {
                    throw new BindingException(
                            "Cannot convert value '" + value + "' to char/Character. Expected exactly one character."
                    );
                }
                return value.charAt(0);
            }
        } catch (NumberFormatException exception) {
            throw new BindingException(
                    "Cannot convert value '" + value + "' to type " + targetType.getName() + ".",
                    exception
            );
        }

        throw new BindingException("Unsupported conversion target type: " + targetType.getName() + ".");
    }
}
