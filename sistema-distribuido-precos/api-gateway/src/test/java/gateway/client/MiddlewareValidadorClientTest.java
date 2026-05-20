package gateway.client;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.model.PrecoPayload;
import gateway.model.ValidationClientResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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

    private static final class FakeRequestor extends Requestor {
        private final RemoteInvocationResponse response;
        private final RuntimeException runtimeException;

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
            return response;
        }
    }
}
