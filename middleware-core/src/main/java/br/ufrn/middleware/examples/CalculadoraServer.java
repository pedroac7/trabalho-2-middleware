package br.ufrn.middleware.examples;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;

public class CalculadoraServer {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.useProtocol(new HttpProtocolPlugin(8080));

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop));

        middleware.start();

        System.out.println("CalculadoraServer iniciado em http://localhost:8080");
        System.out.println("Teste: GET http://localhost:8080/calculadora/soma?a=10&b=20");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
