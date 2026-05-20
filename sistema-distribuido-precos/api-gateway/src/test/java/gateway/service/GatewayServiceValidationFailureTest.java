package gateway.service;

import gateway.client.RepositorioClient;
import gateway.client.ValidadorClient;
import gateway.model.GatewayResult;
import gateway.model.PrecoPayload;
import gateway.model.StorageClientResult;
import gateway.model.ValidationClientResult;
import heartbeat.HeartbeatReceiver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayServiceValidationFailureTest {

    @Test
    void shouldReturnTimeoutValidadorWhenAllValidatorsTimeout() {
        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(9090, "http");
        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
        receiver.getValidadores().put("http://127.0.0.1:8081", System.currentTimeMillis());
        receiver.getRepositorios().put("http://127.0.0.1:8082", System.currentTimeMillis());

        SequencedValidadorClient validadorClient = new SequencedValidadorClient();
        validadorClient.enqueueIOException("REQUEST_TIMEOUT");
        FakeRepositorioClient repositorioClient = new FakeRepositorioClient();

        GatewayService gatewayService = new GatewayService(0, receiver, validadorClient, repositorioClient);
        GatewayResult result = gatewayService.process(new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertEquals(503, result.statusCode());
        assertEquals("TIMEOUT_VALIDADOR", result.mensagem());
        assertEquals(0, repositorioClient.calls);

        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
    }

    @Test
    void shouldReturnSemValidadorDisponivelWhenAllValidatorsAreBusy() {
        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(9090, "http");
        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
        receiver.getValidadores().put("http://127.0.0.1:8081", System.currentTimeMillis());
        receiver.getRepositorios().put("http://127.0.0.1:8082", System.currentTimeMillis());

        SequencedValidadorClient validadorClient = new SequencedValidadorClient();
        validadorClient.enqueueIOException("SERVER_BUSY");
        FakeRepositorioClient repositorioClient = new FakeRepositorioClient();

        GatewayService gatewayService = new GatewayService(0, receiver, validadorClient, repositorioClient);
        GatewayResult result = gatewayService.process(new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertEquals(503, result.statusCode());
        assertEquals("SEM_VALIDADOR_DISPONIVEL", result.mensagem());
        assertEquals(0, repositorioClient.calls);

        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
    }

    @Test
    void shouldFailOverToNextValidatorWhenFirstOneTimesOut() {
        HeartbeatReceiver receiver = HeartbeatReceiver.getInstance(9090, "http");
        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
        receiver.getValidadores().put("http://127.0.0.1:8081", System.currentTimeMillis());
        receiver.getValidadores().put("http://127.0.0.1:8084", System.currentTimeMillis());
        receiver.getRepositorios().put("http://127.0.0.1:8082", System.currentTimeMillis());

        SequencedValidadorClient validadorClient = new SequencedValidadorClient();
        validadorClient.enqueueIOException("REQUEST_TIMEOUT");
        validadorClient.enqueueResult(ValidationClientResult.accepted());
        FakeRepositorioClient repositorioClient = new FakeRepositorioClient();

        GatewayService gatewayService = new GatewayService(0, receiver, validadorClient, repositorioClient);
        GatewayResult result = gatewayService.process(new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertEquals(200, result.statusCode());
        assertEquals("ARMAZENADO", result.mensagem());
        assertTrue(result.replicas() >= 1);
        assertEquals(1, repositorioClient.calls);

        receiver.getValidadores().clear();
        receiver.getRepositorios().clear();
    }

    private static final class SequencedValidadorClient implements ValidadorClient {
        private final Deque<Object> outcomes = new ArrayDeque<>();

        private void enqueueIOException(String message) {
            outcomes.addLast(new IOException(message));
        }

        private void enqueueResult(ValidationClientResult result) {
            outcomes.addLast(result);
        }

        @Override
        public ValidationClientResult validar(String host, int port, PrecoPayload preco) throws IOException {
            return validar("http", host, port, preco);
        }

        @Override
        public ValidationClientResult validar(String protocol, String host, int port, PrecoPayload preco) throws IOException {
            Object outcome = outcomes.isEmpty() ? ValidationClientResult.accepted() : outcomes.removeFirst();
            if (outcome instanceof IOException exception) {
                throw exception;
            }
            return (ValidationClientResult) outcome;
        }
    }

    private static final class FakeRepositorioClient implements RepositorioClient {
        private int calls;

        @Override
        public StorageClientResult armazenar(String host, int port, PrecoPayload preco) {
            calls++;
            return StorageClientResult.success("ARMAZENADO");
        }

        @Override
        public StorageClientResult armazenar(String protocol, String host, int port, PrecoPayload preco) {
            return armazenar(host, port, preco);
        }
    }
}
