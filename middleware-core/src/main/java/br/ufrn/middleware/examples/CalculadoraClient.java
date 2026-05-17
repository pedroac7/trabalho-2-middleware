package br.ufrn.middleware.examples;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

import java.util.Map;

public class CalculadoraClient {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();

        AbsoluteObjectReference ref =
                AbsoluteObjectReference.fromUri("http://localhost:8080/calculadora");

        RemoteInvocationResponse soma = middleware.getRequestor()
                .get(ref, "/soma", Map.of("a", "10", "b", "20"));

        System.out.println("SOMA status=" + soma.getStatusCode());
        System.out.println("SOMA body=" + soma.getBody());

        RemoteInvocationResponse eco = middleware.getRequestor()
                .get(ref, "/eco", Map.of("valor", "teste"));

        System.out.println("ECO status=" + eco.getStatusCode());
        System.out.println("ECO body=" + eco.getBody());

        RemoteInvocationResponse body = middleware.getRequestor()
                .post(ref, "/body", "conteudo");

        System.out.println("BODY status=" + body.getStatusCode());
        System.out.println("BODY body=" + body.getBody());
    }
}
