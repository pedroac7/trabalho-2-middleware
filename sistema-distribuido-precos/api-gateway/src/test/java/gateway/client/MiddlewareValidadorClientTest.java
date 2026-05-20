package gateway.client;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.model.PrecoPayload;
import gateway.model.ValidationClientResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddlewareValidadorClientTest {

    @Test
    void shouldParseSuccessEnvelopeToValidationClientResult() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        200,
                        "{\"success\":true,\"statusCode\":200,\"result\":{\"valid\":true,\"mensagem\":\"VALIDO\"}}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        ValidationClientResult result = client.validar("localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertTrue(result.valid());
        assertEquals("VALIDO", result.mensagem());
    }

    @Test
    void shouldParseBusinessInvalidResultFromEnvelope() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        200,
                        "{\"success\":true,\"statusCode\":200,\"result\":{\"valid\":false,\"mensagem\":\"VALOR_INVALIDO\"}}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        ValidationClientResult result = client.validar("localhost", 8081, new PrecoPayload("PETR4", 0.0, 1710000000000L));

        assertFalse(result.valid());
        assertEquals("VALOR_INVALIDO", result.mensagem());
    }

    @Test
    void shouldReturnInvalidForBadRequestStatus() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        400,
                        "{\"success\":false,\"statusCode\":400,\"error\":\"JSON_INVALIDO\"}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        ValidationClientResult result = client.validar("localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L));

        assertFalse(result.valid());
        assertEquals("JSON_INVALIDO", result.mensagem());
    }

    @Test
    void shouldThrowIOExceptionForCommunicationFailure() {
        FakeRequestor requestor = new FakeRequestor(new RuntimeException("falha"));
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        assertThrows(
                IOException.class,
                () -> client.validar("localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L))
        );
    }

    @Test
    void shouldThrowIOExceptionForServerBusyResponse() {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        503,
                        "{\"success\":false,\"statusCode\":503,\"error\":\"SERVER_BUSY\"}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        IOException exception = assertThrows(
                IOException.class,
                () -> client.validar("localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L))
        );
        assertTrue(exception.getMessage().contains("SERVER_BUSY"));
    }

    @Test
    void shouldThrowIOExceptionForTimeoutResponse() {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        504,
                        "{\"success\":false,\"statusCode\":504,\"error\":\"REQUEST_TIMEOUT\"}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        IOException exception = assertThrows(
                IOException.class,
                () -> client.validar("localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L))
        );
        assertTrue(exception.getMessage().contains("REQUEST_TIMEOUT"));
    }

    @Test
    void shouldBuildReferenceWithAnnouncedProtocol() throws Exception {
        FakeRequestor requestor = new FakeRequestor(
                new RemoteInvocationResponse(
                        200,
                        "{\"success\":true,\"statusCode\":200,\"result\":{\"valid\":true,\"mensagem\":\"VALIDO\"}}"
                )
        );
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(requestor, new ObjectMapper());

        client.validar("tcp", "localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L));
        assertEquals("tcp://localhost:8081/validador", requestor.lastReference.toUri().toString());

        client.validar("udp", "localhost", 8081, new PrecoPayload("PETR4", 35.5, 1710000000000L));
        assertEquals("udp://localhost:8081/validador", requestor.lastReference.toUri().toString());
    }

    @Test
    void shouldCreateRequestorWithProvidedTimeout() throws Exception {
        MiddlewareValidadorClient client = new MiddlewareValidadorClient(3000);

        Field requestorField = MiddlewareValidadorClient.class.getDeclaredField("requestor");
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
