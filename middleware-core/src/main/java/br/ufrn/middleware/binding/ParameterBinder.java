package br.ufrn.middleware.binding;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.error.BindingException;
import br.ufrn.middleware.marshaller.JsonBodyMarshaller;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;

public class ParameterBinder {
    private final SimpleTypeConverter converter;
    private final JsonBodyMarshaller jsonBodyMarshaller;

    public ParameterBinder() {
        this(new SimpleTypeConverter(), new JsonBodyMarshaller());
    }

    public ParameterBinder(SimpleTypeConverter converter) {
        this(converter, new JsonBodyMarshaller());
    }

    public ParameterBinder(SimpleTypeConverter converter, JsonBodyMarshaller jsonBodyMarshaller) {
        if (converter == null) {
            throw new IllegalArgumentException("SimpleTypeConverter must not be null.");
        }
        if (jsonBodyMarshaller == null) {
            throw new IllegalArgumentException("JsonBodyMarshaller must not be null.");
        }
        this.converter = converter;
        this.jsonBodyMarshaller = jsonBodyMarshaller;
    }

    public Object[] bind(RemoteMethodDescriptor descriptor, Map<String, String> queryParams, String body) {
        if (descriptor == null) {
            throw new BindingException("Remote method descriptor must not be null.");
        }

        Map<String, String> safeQueryParams = queryParams == null ? Collections.emptyMap() : queryParams;

        Method method = descriptor.getJavaMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        int bodyParameterCount = 0;

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
                bodyParameterCount++;
                if (bodyParameterCount > 1) {
                    throw new BindingException(
                            "Method " + method.getName() + " cannot have more than one @Body parameter."
                    );
                }

                if (parameter.getType() != String.class) {
                    args[i] = jsonBodyMarshaller.unmarshalBody(body, parameter.getType());
                    continue;
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
