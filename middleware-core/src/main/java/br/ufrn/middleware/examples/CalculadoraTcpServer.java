package br.ufrn.middleware.examples;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.TcpProtocolPlugin;

public class CalculadoraTcpServer {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();
        middleware.register(new CalculadoraService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new TcpProtocolPlugin(9090));

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop));

        middleware.start();

        System.out.println("CalculadoraTcpServer iniciado em tcp://localhost:9090");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
