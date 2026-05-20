package br.ufrn.middleware.client;

import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.error.RemotingException;

import java.util.HashMap;
import java.util.Map;

public class Requestor {
    private final Map<String, ClientRequestHandler> handlersByProtocol;

    public Requestor() {
        this.handlersByProtocol = new HashMap<>();
        registerHandler("http", new HttpClientRequestHandler());
        registerHandler("tcp", new TcpClientRequestHandler());
        registerHandler("udp", new UdpClientRequestHandler());
    }

    public Requestor(int timeoutMillis) {
        this.handlersByProtocol = new HashMap<>();
        registerHandler("http", new HttpClientRequestHandler(timeoutMillis));
        registerHandler("tcp", new TcpClientRequestHandler(timeoutMillis));
        registerHandler("udp", new UdpClientRequestHandler(timeoutMillis));
    }

    public Requestor(ClientRequestHandler clientRequestHandler) {
        if (clientRequestHandler == null) {
            throw new IllegalArgumentException("ClientRequestHandler must not be null.");
        }
        this.handlersByProtocol = new HashMap<>();
        registerHandler("http", clientRequestHandler);
    }

    public RemoteInvocationResponse invoke(RemoteInvocationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RemoteInvocationRequest must not be null.");
        }

        String protocol = request.getObjectReference().getProtocol();
        if (protocol == null || protocol.isBlank()) {
            throw new RemotingException("Object reference protocol must not be null or blank.");
        }

        ClientRequestHandler handler = handlersByProtocol.get(protocol.trim().toLowerCase());
        if (handler == null) {
            throw new RemotingException("No client request handler registered for protocol: " + protocol);
        }

        return handler.send(request);
    }

    public RemoteInvocationResponse get(
            AbsoluteObjectReference reference,
            String remotePath,
            Map<String, String> queryParams
    ) {
        validateReferenceAndPath(reference, remotePath);
        return invoke(new RemoteInvocationRequest(reference, "GET", remotePath, queryParams, null));
    }

    public RemoteInvocationResponse post(
            AbsoluteObjectReference reference,
            String remotePath,
            String body
    ) {
        validateReferenceAndPath(reference, remotePath);
        return invoke(new RemoteInvocationRequest(reference, "POST", remotePath, Map.of(), body));
    }

    public Requestor registerHandler(String protocol, ClientRequestHandler handler) {
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Protocol must not be null or blank.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("ClientRequestHandler must not be null.");
        }

        handlersByProtocol.put(protocol.trim().toLowerCase(), handler);
        return this;
    }

    private void validateReferenceAndPath(AbsoluteObjectReference reference, String remotePath) {
        if (reference == null) {
            throw new IllegalArgumentException("AbsoluteObjectReference must not be null.");
        }
        if (remotePath == null || remotePath.isBlank()) {
            throw new IllegalArgumentException("Remote path must not be null or blank.");
        }
    }
}
