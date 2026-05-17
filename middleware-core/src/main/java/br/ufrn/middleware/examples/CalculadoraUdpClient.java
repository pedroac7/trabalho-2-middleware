package br.ufrn.middleware.examples;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.identification.AbsoluteObjectReference;

public class CalculadoraUdpClient {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();
        AbsoluteObjectReference reference =
                AbsoluteObjectReference.fromUri("udp://localhost:9091/calculadora");

        RemoteInvocationResponse soma = middleware.getRequestor()
                .get(reference, "/soma", java.util.Map.of("a", "10", "b", "20"));
        System.out.println("SOMA status=" + soma.getStatusCode());
        System.out.println("SOMA body=" + soma.getBody());

        RemoteInvocationResponse eco = middleware.getRequestor()
                .get(reference, "/eco", java.util.Map.of("valor", "teste"));
        System.out.println("ECO status=" + eco.getStatusCode());
        System.out.println("ECO body=" + eco.getBody());

        RemoteInvocationResponse body = middleware.getRequestor()
                .post(reference, "/body", "conteudo");
        System.out.println("BODY status=" + body.getStatusCode());
        System.out.println("BODY body=" + body.getBody());
    }
}
