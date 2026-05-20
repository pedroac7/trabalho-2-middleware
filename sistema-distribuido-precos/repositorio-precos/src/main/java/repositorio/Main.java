package repositorio;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.TcpProtocolPlugin;
import br.ufrn.middleware.protocol.UdpProtocolPlugin;
import heartbeat.HeartbeatSender;
import repositorio.service.RepositorioService;

public class Main {
    private static final int REPOSITORIO_WORKER_THREADS = 100;
    private static final int REPOSITORIO_QUEUE_CAPACITY = 300;

    public static void main(String[] args) {
        String serviceProtocol = args.length > 0 ? args[0] : "http";
        int servicePort = args.length > 1 ? Integer.parseInt(args[1]) : 8082;
        String gatewayHost = args.length > 2 ? args[2] : "localhost";
        int gatewayHeartbeatPort = args.length > 3 ? Integer.parseInt(args[3]) : 9090;

        Middleware middleware = new Middleware();
        middleware.register(new RepositorioService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(
                createProtocolPlugin(
                        serviceProtocol,
                        servicePort,
                        REPOSITORIO_WORKER_THREADS,
                        REPOSITORIO_QUEUE_CAPACITY
                )
        );
        middleware.start();

        HeartbeatSender heartbeatSender = new HeartbeatSender(
                gatewayHost,
                gatewayHeartbeatPort,
                servicePort,
                "http",
                "repositorio",
                serviceProtocol
        );
        Thread heartbeatThread = new Thread(heartbeatSender, "repositorio-middleware-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        System.out.println("[Repositorio] Protocolo do servico: " + normalizeProtocol(serviceProtocol));
        System.out.println("[Repositorio] Servico remoto em " + normalizeProtocol(serviceProtocol) + "://localhost:" + servicePort + "/repositorio");
        System.out.println("[Repositorio] Endpoint: POST /repositorio/armazenar");
        System.out.println("[Repositorio] Worker threads: " + REPOSITORIO_WORKER_THREADS);
        System.out.println("[Repositorio] Capacidade da fila: " + REPOSITORIO_QUEUE_CAPACITY);
        System.out.println(
                "[Repositorio] Heartbeat iniciado para gateway "
                        + gatewayHost + ":" + gatewayHeartbeatPort
                        + " anunciando " + normalizeProtocol(serviceProtocol)
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
