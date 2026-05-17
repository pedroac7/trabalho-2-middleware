package br.ufrn.middleware.error;

public class BindingException extends RemotingException {
    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
