package br.ufrn.middleware.invoker;

import br.ufrn.middleware.error.InvocationException;
import br.ufrn.middleware.registry.RemoteMethodDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Invoker {
    public Object invoke(RemoteMethodDescriptor descriptor, Object[] args) {
        if (descriptor == null) {
            throw new InvocationException("Remote method descriptor must not be null.");
        }

        Object[] invocationArgs = args == null ? new Object[0] : args;
        Method javaMethod = descriptor.getJavaMethod();
        Object targetInstance = descriptor.getTargetInstance();

        int expectedArgs = javaMethod.getParameterCount();
        if (invocationArgs.length != expectedArgs) {
            throw new InvocationException(
                    "Invalid argument count for "
                            + javaMethod.getDeclaringClass().getName()
                            + "."
                            + javaMethod.getName()
                            + ": expected "
                            + expectedArgs
                            + " but got "
                            + invocationArgs.length
                            + "."
            );
        }

        try {
            return javaMethod.invoke(targetInstance, invocationArgs);
        } catch (IllegalArgumentException exception) {
            throw new InvocationException(
                    "Invalid argument type while invoking "
                            + javaMethod.getDeclaringClass().getName()
                            + "."
                            + javaMethod.getName()
                            + ".",
                    exception
            );
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new InvocationException(
                    "Remote method threw an exception: "
                            + javaMethod.getDeclaringClass().getName()
                            + "."
                            + javaMethod.getName()
                            + ".",
                    cause
            );
        } catch (IllegalAccessException exception) {
            throw new InvocationException(
                    "Could not access remote method "
                            + javaMethod.getDeclaringClass().getName()
                            + "."
                            + javaMethod.getName()
                            + ".",
                    exception
            );
        }
    }
}
