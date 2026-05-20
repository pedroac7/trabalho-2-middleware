package validador;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import heartbeat.HeartbeatSender;
import validador.service.ValidadorService;

public class Main {

    public static void main(String[] args) {
        int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        String gatewayHost = args.length > 1 ? args[1] : "localhost";
        int gatewayHeartbeatPort = args.length > 2 ? Integer.parseInt(args[2]) : 9090;
        String announcedProtocol = args.length > 3 ? args[3] : "middleware";

        Middleware middleware = new Middleware();
        middleware.register(new ValidadorService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new HttpProtocolPlugin(servicePort));
        middleware.start();

        HeartbeatSender heartbeatSender = new HeartbeatSender(
                gatewayHost,
                gatewayHeartbeatPort,
                servicePort,
                "http",
                "validador",
                announcedProtocol
        );
        Thread heartbeatThread = new Thread(heartbeatSender, "validador-middleware-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        System.out.println("ValidadorMain iniciado em http://localhost:" + servicePort + "/validador");
        System.out.println("Endpoint: POST /validador/validar");
        System.out.println(
                "Heartbeat do validador iniciado para gateway "
                        + gatewayHost + ":" + gatewayHeartbeatPort
                        + " anunciando " + announcedProtocol
                        + " na porta " + servicePort
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            heartbeatSender.stop();
            middleware.stop();
        }, "validador-middleware-shutdown"));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}