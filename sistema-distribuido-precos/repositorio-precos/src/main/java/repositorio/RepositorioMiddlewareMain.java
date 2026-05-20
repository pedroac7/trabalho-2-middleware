package repositorio;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import heartbeat.HeartbeatSender;
import repositorio.service.RepositorioService;

public class RepositorioMiddlewareMain {
    public static void main(String[] args) {
        int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8082;
        String gatewayHost = args.length > 1 ? args[1] : "localhost";
        int gatewayHeartbeatPort = args.length > 2 ? Integer.parseInt(args[2]) : 9090;
        String announcedProtocol = args.length > 3 ? args[3] : "middleware";

        Middleware middleware = new Middleware();
        middleware.register(new RepositorioService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new HttpProtocolPlugin(servicePort));
        middleware.start();

        HeartbeatSender heartbeatSender = new HeartbeatSender(
                gatewayHost,
                gatewayHeartbeatPort,
                servicePort,
                "http",
                "repositorio",
                announcedProtocol
        );
        Thread heartbeatThread = new Thread(heartbeatSender, "repositorio-middleware-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        System.out.println("RepositorioMiddlewareMain iniciado em http://localhost:" + servicePort + "/repositorio");
        System.out.println("Endpoint: POST /repositorio/armazenar");
        System.out.println(
                "Heartbeat do repositorio iniciado para gateway "
                        + gatewayHost + ":" + gatewayHeartbeatPort
                        + " anunciando " + announcedProtocol
                        + " na porta " + servicePort
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            heartbeatSender.stop();
            middleware.stop();
        }, "repositorio-middleware-shutdown"));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}