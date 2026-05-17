package br.ufrn.middleware.marshaller;

import br.ufrn.middleware.error.RemotingException;

public class MarshallingException extends RemotingException {
    public MarshallingException(String message) {
        super(message);
    }

    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }
}
