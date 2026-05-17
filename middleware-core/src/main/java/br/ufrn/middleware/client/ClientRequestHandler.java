package br.ufrn.middleware.client;

public interface ClientRequestHandler {
    RemoteInvocationResponse send(RemoteInvocationRequest request);
}
