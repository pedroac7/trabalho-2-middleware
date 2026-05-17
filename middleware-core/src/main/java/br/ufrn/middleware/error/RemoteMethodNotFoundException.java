package br.ufrn.middleware.error;

public class RemoteMethodNotFoundException extends RemotingException {
    public RemoteMethodNotFoundException(String message) {
        super(message);
    }

    public RemoteMethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
