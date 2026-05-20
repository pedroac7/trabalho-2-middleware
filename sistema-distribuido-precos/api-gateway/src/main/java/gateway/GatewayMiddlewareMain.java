package gateway;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import gateway.client.RepositorioClient;
import gateway.client.ValidadorClient;
import gateway.factory.RepositorioClientFactory;
import gateway.factory.ValidadorClientFactory;
import gateway.service.GatewayService;
import heartbeat.HeartbeatReceiver;

public class GatewayMiddlewareMain {
    public static void main(String[] args) throws InterruptedException {
        int gatewayServicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int heartbeatPort = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        long epsilonMillis = args.length > 2 ? Math.max(0L, Long.parseLong(args[2])) : 100L;
        String heartbeatProtocol = args.length > 3 ? args[3] : "http";
        String internalClientsProtocol = args.length > 4 ? args[4] : "middleware";

        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(heartbeatPort, heartbeatProtocol);
        receiver.start();

        ValidadorClient validadorClient =
                ValidadorClientFactory.create(internalClientsProtocol, GatewayService.socketTimeoutMs());
        RepositorioClient repositorioClient =
                RepositorioClientFactory.create(internalClientsProtocol, GatewayService.socketTimeoutMs());
        GatewayService gatewayService = new GatewayService(epsilonMillis, receiver, validadorClient, repositorioClient);

        Middleware middleware = new Middleware();
        middleware.register(gatewayService);
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(new HttpProtocolPlugin(gatewayServicePort));
        middleware.start();

        System.out.println("[GatewayMiddleware] HeartbeatReceiver iniciado na porta " + heartbeatPort + " usando protocolo " + heartbeatProtocol);
        System.out.println("[GatewayMiddleware] Clients internos configurados com protocolo " + internalClientsProtocol);
        System.out.println("[GatewayMiddleware] EPSILON configurado em " + epsilonMillis + "ms");
        System.out.println("[GatewayMiddleware] Gateway exposto via middleware em http://localhost:" + gatewayServicePort + "/gateway");
        System.out.println("[GatewayMiddleware] Endpoint: POST /gateway/precos");

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop, "gateway-middleware-shutdown"));
        Thread.currentThread().join();
    }
}
