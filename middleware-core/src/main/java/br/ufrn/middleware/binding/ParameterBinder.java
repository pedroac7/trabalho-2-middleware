package br.ufrn.middleware.binding;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;

public class ParameterBinder {
    private final SimpleTypeConverter converter;

    public ParameterBinder() {
        this(new SimpleTypeConverter());
    }

    public ParameterBinder(SimpleTypeConverter converter) {
        if (converter == null) {
            throw new IllegalArgumentException("SimpleTypeConverter must not be null.");
        }
        this.converter = converter;
    }

    public Object[] bind(RemoteMethodDescriptor descriptor, Map<String, String> queryParams, String body) {
        if (descriptor == null) {
            throw new BindingException("Remote method descriptor must not be null.");
        }

        Map<String, String> safeQueryParams = queryParams == null ? Collections.emptyMap() : queryParams;

        Method method = descriptor.getJavaMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Param paramAnnotation = parameter.getAnnotation(Param.class);
            Body bodyAnnotation = parameter.getAnnotation(Body.class);

            if (paramAnnotation != null && bodyAnnotation != null) {
                throw new BindingException(
                        "Parameter '" + parameter.getName() + "' in method "
                                + method.getName()
                                + " cannot have both @Param and @Body."
                );
            }

            if (paramAnnotation != null) {
                String paramName = paramAnnotation.value();
                if (paramName == null || paramName.isBlank()) {
                    throw new BindingException(
                            "Parameter '" + parameter.getName() + "' in method "
                                    + method.getName()
                                    + " has empty @Param value."
                    );
                }

                String rawValue = safeQueryParams.get(paramName);
                if (!safeQueryParams.containsKey(paramName) && parameter.getType().isPrimitive()) {
                    throw new BindingException(
                            "Missing required query parameter '" + paramName + "' for primitive type "
                                    + parameter.getType().getName()
                                    + "."
                    );
                }

                args[i] = converter.convert(rawValue, parameter.getType());
                continue;
            }

            if (bodyAnnotation != null) {
                if (parameter.getType() != String.class) {
                    throw new BindingException(
                            "Body binding for type "
                                    + parameter.getType().getName()
                                    + " is not implemented yet. Only String is supported."
                    );
                }
                args[i] = body;
                continue;
            }

            throw new BindingException(
                    "Parameter '" + parameter.getName() + "' in method "
                            + method.getName()
                            + " must be annotated with @Param or @Body."
            );
        }

        return args;
    }
}
