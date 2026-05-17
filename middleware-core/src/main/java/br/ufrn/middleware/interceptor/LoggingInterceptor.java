package br.ufrn.middleware.interceptor;

import java.time.Duration;
import java.time.Instant;

public class LoggingInterceptor implements InvocationInterceptor {
    @Override
    public void before(InvocationContext context) {
        System.out.println(
                "["
                        + context.getRequestId()
                        + "] BEFORE "
                        + context.getHttpMethod()
                        + " "
                        + context.getPath()
        );
    }

    @Override
    public void after(InvocationContext context) {
        Instant end = context.getFinishedAt() != null ? context.getFinishedAt() : Instant.now();
        long durationMs = Duration.between(context.getStartedAt(), end).toMillis();

        System.out.println(
                "["
                        + context.getRequestId()
                        + "] AFTER status="
                        + context.getStatusCode()
                        + " durationMs="
                        + durationMs
        );
    }

    @Override
    public void onError(InvocationContext context, Throwable error) {
        String message = error == null ? "unknown error" : error.getMessage();
        System.out.println("[" + context.getRequestId() + "] ERROR " + message);
    }
}
