package gateway.client;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.model.PrecoPayload;
import gateway.model.StorageClientResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddlewareRepositorioClientTest {

    @Test
    void shouldParseSuccessEnvelopeToStorageClientResult() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        200,
                        "{\"success\":true,\"statusCode\":200,\"result\":{\"success\":true,\"mensagem\":\"ARMAZENADO\"}}"
                )
        );
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(requestor, new ObjectMapper());

        StorageClientResult result = client.armazenar("localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertTrue(result.success());
        assertEquals("ARMAZENADO", result.mensagem());
    }

    @Test
    void shouldReturnErrorForFailureEnvelope() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        500,
                        "{\"success\":false,\"statusCode\":500,\"error\":\"FALHA_AO_ARMAZENAR\"}"
                )
        );
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(requestor, new ObjectMapper());

        StorageClientResult result = client.armazenar("localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertFalse(result.success());
        assertEquals("FALHA_AO_ARMAZENAR", result.mensagem());
    }

    @Test
    void shouldReturnErrorForInvalidEnvelopeBody() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(200, "invalid-json")
        );
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(requestor, new ObjectMapper());

        StorageClientResult result = client.armazenar("localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertFalse(result.success());
        assertEquals("RESPOSTA_INVALIDA_DO_REPOSITORIO", result.mensagem());
    }

    @Test
    void shouldThrowIOExceptionForCommunicationFailure() {
        FakeRequestor requestor = new FakeRequestor(new RuntimeException("falha"));
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(requestor, new ObjectMapper());

        assertThrows(
                IOException.class,
                () -> client.armazenar("localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L))
        );
    }

    @Test
    void shouldBuildReferenceWithAnnouncedProtocol() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        200,
                        "{\"success\":true,\"statusCode\":200,\"result\":{\"success\":true,\"mensagem\":\"ARMAZENADO\"}}"
                )
        );
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(requestor, new ObjectMapper());

        client.armazenar("tcp", "localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L));
        assertEquals("tcp://localhost:8082/repositorio", requestor.lastReference.toUri().toString());

        client.armazenar("udp", "localhost", 8082, new PrecoPayload("PETR4", 35.5, 1710000000000L));
        assertEquals("udp://localhost:8082/repositorio", requestor.lastReference.toUri().toString());
    }

    @Test
    void shouldCreateRequestorWithProvidedTimeout() throws Exception {
        MiddlewareRepositorioClient client = new MiddlewareRepositorioClient(3000);

        Field requestorField = MiddlewareRepositorioClient.class.getDeclaredField("requestor");
        requestorField.setAccessible(true);
        Requestor requestor = (Requestor) requestorField.get(client);

        Field handlersField = Requestor.class.getDeclaredField("handlersByProtocol");
        handlersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> handlers = (Map<String, Object>) handlersField.get(requestor);

        assertEquals(3000, readTimeoutField(handlers.get("http")));
        assertEquals(3000, readTimeoutField(handlers.get("tcp")));
        assertEquals(3000, readTimeoutField(handlers.get("udp")));
    }

    private int readTimeoutField(Object handler) throws Exception {
        Field timeoutField = handler.getClass().getDeclaredField("timeoutMillis");
        timeoutField.setAccessible(true);
        return timeoutField.getInt(handler);
    }

    private static final class FakeRequestor extends Requestor {
        private final RemoteInvocationResponse response;
        private final RuntimeException runtimeException;
        private AbsoluteObjectReference lastReference;

        private FakeRequestor(RemoteInvocationResponse response) {
            this.response = response;
            this.runtimeException = null;
        }

        private FakeRequestor(RuntimeException runtimeException) {
            this.response = null;
            this.runtimeException = runtimeException;
        }

        @Override
        public RemoteInvocationResponse post(AbsoluteObjectReference reference, String remotePath, String body) {
            if (runtimeException != null) {
                throw runtimeException;
            }
            this.lastReference = reference;
            return response;
        }
    }
}
