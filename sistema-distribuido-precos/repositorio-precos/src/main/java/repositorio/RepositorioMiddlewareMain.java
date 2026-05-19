package repositorio;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import repositorio.service.RepositorioService;

public class RepositorioMiddlewareMain {
    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8082;

        Middleware middleware = new Middleware();
        middleware.register(new RepositorioService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new HttpProtocolPlugin(porta));

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop));

        middleware.start();

        System.out.println("RepositorioMiddlewareMain iniciado em http://localhost:" + porta + "/repositorio");
        System.out.println("Endpoint: POST /repositorio/armazenar");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
