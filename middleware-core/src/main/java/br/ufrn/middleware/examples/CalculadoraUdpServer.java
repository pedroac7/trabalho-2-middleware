package br.ufrn.middleware.examples;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.UdpProtocolPlugin;

public class CalculadoraUdpServer {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new UdpProtocolPlugin(9091));

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop));

        middleware.start();

        System.out.println("CalculadoraUdpServer iniciado em udp://localhost:9091");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
