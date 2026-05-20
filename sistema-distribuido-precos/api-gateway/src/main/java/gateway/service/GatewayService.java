package gateway.service;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Lifecycle;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.lifecycle.LifecycleType;
import gateway.client.*;
import gateway.model.*;

import heartbeat.HeartbeatReceiver;
import heartbeat.HeartbeatReceiver.ServiceInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RemoteComponent("gateway")
@Lifecycle(LifecycleType.STATIC_INSTANCE)
public class GatewayService {
    private static final int SOCKET_TIMEOUT_MS = 2000;

    private final long epsilonMillis;
    private final HeartbeatReceiver heartbeatReceiver;
    private final ValidadorClient validadorClient;
    private final RepositorioClient repositorioClient;
    private final AtomicInteger nextValidadorIndex = new AtomicInteger();

    public GatewayService(long epsilonMillis,
                          HeartbeatReceiver heartbeatReceiver,
                          ValidadorClient validadorClient,
                          RepositorioClient repositorioClient) {
        this.epsilonMillis = epsilonMillis;
        this.heartbeatReceiver = heartbeatReceiver;
        this.validadorClient = validadorClient;
        this.repositorioClient = repositorioClient;
    }

    public static int socketTimeoutMs() {
        return SOCKET_TIMEOUT_MS;
    }

    @RemoteMethod(method = "POST", path = "/precos")
    public GatewayResult process(@Body PrecoPayload preco) {
        List<RemoteEndpoint> validadores = snapshotEndpoints(heartbeatReceiver.getValidadores());
        if (validadores.isEmpty()) {
            return GatewayResult.error(503, "SEM_VALIDADOR_DISPONIVEL");
        }

        ValidationAttempt validationAttempt = validate(preco, validadores);
        if (!validationAttempt.available()) {
            return GatewayResult.error(503, validationAttempt.message());
        }
        if (!validationAttempt.valid()) {
            return GatewayResult.error(400, validationAttempt.message());
        }

        List<RemoteEndpoint> repositorios = snapshotEndpoints(heartbeatReceiver.getRepositorios());
        if (repositorios.isEmpty()) {
            return GatewayResult.error(503, "SEM_REPOSITORIO_DISPONIVEL");
        }

        int replicasComSucesso = replicate(preco, repositorios);
        if (replicasComSucesso == 0) {
            return GatewayResult.error(503, "FALHA_AO_REPLICAR");
        }

        long waitMillis = 2 * epsilonMillis;
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GatewayResult.error(503, "INTERRUPTADO_NO_CLOCK_BOUND_WAIT");
        }

        return GatewayResult.success(replicasComSucesso);
    }

    private ValidationAttempt validate(PrecoPayload preco, List<RemoteEndpoint> validadores) {
        int startIndex = Math.floorMod(nextValidadorIndex.getAndIncrement(), validadores.size());
        boolean timeoutDetected = false;
        for (int attempt = 0; attempt < validadores.size(); attempt++) {
            RemoteEndpoint validador = validadores.get((startIndex + attempt) % validadores.size());
            try {
                ValidationClientResult result = validadorClient.validar(
                        validador.protocol(),
                        validador.host(),
                        validador.port(),
                        preco
                );
                if (result.valid()) {
                    return ValidationAttempt.accepted();
                }
                return ValidationAttempt.invalid(result.mensagem());
            } catch (IOException e) {
                if (isTimeoutMessage(e.getMessage())) {
                    timeoutDetected = true;
                }
            }
        }
        if (timeoutDetected) {
            return ValidationAttempt.unavailable("TIMEOUT_VALIDADOR");
        }
        return ValidationAttempt.unavailable("SEM_VALIDADOR_DISPONIVEL");
    }

    private int replicate(PrecoPayload preco, List<RemoteEndpoint> repositorios) {
        int sucessos = 0;

        for (RemoteEndpoint repositorio : repositorios) {
            try {
                StorageClientResult result = repositorioClient.armazenar(
                        repositorio.protocol(),
                        repositorio.host(),
                        repositorio.port(),
                        preco
                );
                if (result.success()) {
                    sucessos++;
                }
            } catch (IOException e) {
            }
        }

        return sucessos;
    }

    private List<RemoteEndpoint> snapshotEndpoints(Map<String, Long> instances) {
        List<RemoteEndpoint> endpoints = new ArrayList<>();
        for (String key : instances.keySet()) {
            RemoteEndpoint.fromKey(key).ifPresent(endpoints::add);
        }
        endpoints.sort(Comparator.comparing(RemoteEndpoint::address));
        return endpoints;
    }

    private record RemoteEndpoint(String protocol, String host, int port) {
        private static java.util.Optional<RemoteEndpoint> fromKey(String key) {
            return ServiceInstance.fromKey(key)
                    .map(instance -> new RemoteEndpoint(instance.protocol(), instance.host(), instance.port()));
        }

        private String address() {
            return protocol + "://" + host + ":" + port;
        }
    }

    private record ValidationAttempt(boolean available, boolean valid, String message) {
        private static ValidationAttempt accepted() {
            return new ValidationAttempt(true, true, "VALIDO");
        }

        private static ValidationAttempt invalid(String message) {
            return new ValidationAttempt(true, false, message);
        }

        private static ValidationAttempt unavailable(String message) {
            return new ValidationAttempt(false, false, message);
        }
    }

    private boolean isTimeoutMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.trim().toUpperCase();
        return normalized.contains("REQUEST_TIMEOUT") || normalized.contains("TIMEOUT");
    }
}
