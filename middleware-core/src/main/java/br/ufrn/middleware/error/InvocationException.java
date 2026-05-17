package br.ufrn.middleware.error;

public class InvocationException extends RemotingException {
    public InvocationException(String message) {
        super(message);
    }

    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
