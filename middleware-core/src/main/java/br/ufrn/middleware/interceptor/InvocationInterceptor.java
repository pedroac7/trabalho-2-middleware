package br.ufrn.middleware.interceptor;

public interface InvocationInterceptor {
    default void before(InvocationContext context) {
    }

    default void after(InvocationContext context) {
    }

    default void onError(InvocationContext context, Throwable error) {
    }
}
