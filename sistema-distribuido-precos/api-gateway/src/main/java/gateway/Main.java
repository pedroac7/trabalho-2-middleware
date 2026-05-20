package gateway;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.TcpProtocolPlugin;
import br.ufrn.middleware.protocol.UdpProtocolPlugin;
import gateway.client.MiddlewareRepositorioClient;
import gateway.client.MiddlewareValidadorClient;
import gateway.client.RepositorioClient;
import gateway.client.ValidadorClient;
import gateway.service.GatewayService;
import heartbeat.HeartbeatReceiver;

public class Main {
    private static final int INTERNAL_CLIENT_TIMEOUT_MILLIS = 3000;
    private static final int GATEWAY_WORKER_THREADS = 300;
    private static final int GATEWAY_QUEUE_CAPACITY = 1000;

    public static void main(String[] args) throws InterruptedException {
        String gatewayProtocol = args.length > 0 ? args[0] : "http";
        int gatewayServicePort = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        int heartbeatPort = args.length > 2 ? Integer.parseInt(args[2]) : 9090;
        long epsilonMillis = args.length > 3 ? Math.max(0L, Long.parseLong(args[3])) : 100L;
        String internalClientsProtocol = args.length > 4 ? args[4] : normalizeProtocol(gatewayProtocol);
        String heartbeatProtocol = "http";

        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(heartbeatPort, heartbeatProtocol);
        receiver.start();

        ValidadorClient validadorClient = createValidadorClient(internalClientsProtocol);
        RepositorioClient repositorioClient = createRepositorioClient(internalClientsProtocol);
        GatewayService gatewayService = new GatewayService(epsilonMillis, receiver, validadorClient, repositorioClient);

        Middleware middleware = new Middleware();
        middleware.register(gatewayService);
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(
                createProtocolPlugin(
                        gatewayProtocol,
                        gatewayServicePort,
                        GATEWAY_WORKER_THREADS,
                        GATEWAY_QUEUE_CAPACITY
                )
        );
        middleware.start();

        System.out.println("[Gateway] Protocolo do gateway: " + normalizeProtocol(gatewayProtocol));
        System.out.println("[Gateway] HeartbeatReceiver iniciado na porta " + heartbeatPort + " usando protocolo " + heartbeatProtocol);
        System.out.println("[Gateway] Clients internos configurados com protocolo " + internalClientsProtocol);
        System.out.println("[Gateway] Timeout fixo dos clients internos: " + INTERNAL_CLIENT_TIMEOUT_MILLIS + "ms");
        System.out.println("[Gateway] Worker threads do gateway: " + GATEWAY_WORKER_THREADS);
        System.out.println("[Gateway] Capacidade da fila do gateway: " + GATEWAY_QUEUE_CAPACITY);
        System.out.println("[Gateway] EPSILON configurado em " + epsilonMillis + "ms");
        System.out.println("[Gateway] Gateway exposto via middleware em " + normalizeProtocol(gatewayProtocol) + "://localhost:" + gatewayServicePort + "/gateway");
        System.out.println("[Gateway] Endpoint: POST /gateway/precos");

        Runtime.getRuntime().addShutdownHook(new Thread(middleware::stop, "gateway-middleware-shutdown"));
        Thread.currentThread().join();
    }

    private static ValidadorClient createValidadorClient(String protocol) {
        normalizeProtocol(protocol);
        return new MiddlewareValidadorClient(INTERNAL_CLIENT_TIMEOUT_MILLIS);
    }

    private static RepositorioClient createRepositorioClient(String protocol) {
        normalizeProtocol(protocol);
        return new MiddlewareRepositorioClient(INTERNAL_CLIENT_TIMEOUT_MILLIS);
    }

    private static ProtocolPlugin createProtocolPlugin(
            String protocol,
            int port,
            int workerThreads,
            int queueCapacity
    ) {
        return switch (normalizeProtocol(protocol)) {
            case "http" -> new HttpProtocolPlugin(port, workerThreads, queueCapacity);
            case "tcp" -> new TcpProtocolPlugin(port, workerThreads, queueCapacity);
            case "udp" -> new UdpProtocolPlugin(port, workerThreads, queueCapacity);
            default -> throw new IllegalArgumentException("Protocolo nao suportado: " + protocol + ". Use http, tcp ou udp.");
        };
    }

    private static String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Protocolo nao suportado: " + protocol + ". Use http, tcp ou udp.");
        }

        String normalized = protocol.trim().toLowerCase();
        return switch (normalized) {
            case "http", "tcp", "udp" -> normalized;
            default -> throw new IllegalArgumentException("Protocolo nao suportado: " + protocol + ". Use http, tcp ou udp.");
        };
    }
}
