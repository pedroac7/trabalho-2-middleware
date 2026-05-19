package validador;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import validador.service.ValidadorService;

public class ValidadorMiddlewareMain {

    public static void main(String[] args) throws InterruptedException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8081;

        Middleware middleware = new Middleware();
        middleware.register(new ValidadorService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new HttpProtocolPlugin(porta));
        middleware.start();

        System.out.println("ValidadorMiddlewareMain iniciado em http://localhost:" + porta + "/validador");
        System.out.println("Endpoint: POST /validador/validar");

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop, "validador-middleware-shutdown"));

        Thread.currentThread().join();
    }
}