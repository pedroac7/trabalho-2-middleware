package validador;

import br.ufrn.middleware.core.Middleware;
import br.ufrn.middleware.interceptor.LoggingInterceptor;
import br.ufrn.middleware.protocol.HttpProtocolPlugin;
import br.ufrn.middleware.protocol.ProtocolPlugin;
import br.ufrn.middleware.protocol.TcpProtocolPlugin;
import br.ufrn.middleware.protocol.UdpProtocolPlugin;
import heartbeat.HeartbeatSender;
import validador.service.ValidadorService;

public class Main {
    private static final int VALIDADOR_WORKER_THREADS = 40;
    private static final int VALIDADOR_QUEUE_CAPACITY = 80;

    public static void main(String[] args) {
        String serviceProtocol = args.length > 0 ? args[0] : "http";
        int servicePort = args.length > 1 ? Integer.parseInt(args[1]) : 8081;
        String gatewayHost = args.length > 2 ? args[2] : "localhost";
        int gatewayHeartbeatPort = args.length > 3 ? Integer.parseInt(args[3]) : 9090;

        Middleware middleware = new Middleware();
        middleware.register(new ValidadorService());
        middleware.addInterceptor(new LoggingInterceptor());
        middleware.useProtocol(
                createProtocolPlugin(
                        serviceProtocol,
                        servicePort,
                        VALIDADOR_WORKER_THREADS,
                        VALIDADOR_QUEUE_CAPACITY
                )
        );
        middleware.start();

        HeartbeatSender heartbeatSender = new HeartbeatSender(
                gatewayHost,
                gatewayHeartbeatPort,
                servicePort,
                "http",
                "validador",
                serviceProtocol
        );
        Thread heartbeatThread = new Thread(heartbeatSender, "validador-middleware-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();

        System.out.println("[Validador] Protocolo do servico: " + normalizeProtocol(serviceProtocol));
        System.out.println("[Validador] Servico remoto em " + normalizeProtocol(serviceProtocol) + "://localhost:" + servicePort + "/validador");
        System.out.println("[Validador] Endpoint: POST /validador/validar");
        System.out.println(
                "[Validador] Heartbeat iniciado para gateway "
                        + gatewayHost + ":" + gatewayHeartbeatPort
                        + " anunciando " + normalizeProtocol(serviceProtocol)
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
