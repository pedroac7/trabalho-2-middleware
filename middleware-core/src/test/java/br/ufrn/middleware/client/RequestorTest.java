package br.ufrn.middleware.client;

import br.ufrn.middleware.identification.AbsoluteObjectReference;
import br.ufrn.middleware.identification.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestorTest {

    static class FakeClientRequestHandler implements ClientRequestHandler {
        private RemoteInvocationRequest lastRequest;

        @Override
        public RemoteInvocationResponse send(RemoteInvocationRequest request) {
            this.lastRequest = request;
            return new RemoteInvocationResponse(200, "ok");
        }
    }

    @Test
    void requestorGetBuildsExpectedRequest() {
        FakeClientRequestHandler fakeHandler = new FakeClientRequestHandler();
        Requestor requestor = new Requestor(fakeHandler);
        AbsoluteObjectReference reference = new AbsoluteObjectReference(
                "http",
                "localhost",
                8080,
                ObjectId.of("calculadora")
        );

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("a", "10");
        queryParams.put("b", "20");

        RemoteInvocationResponse response = requestor.get(reference, "soma", queryParams);

        assertEquals(200, response.getStatusCode());
        assertEquals("ok", response.getBody());
        assertEquals("GET", fakeHandler.lastRequest.getHttpMethod());
        assertEquals("/soma", fakeHandler.lastRequest.getRemotePath());
        assertEquals(Map.of("a", "10", "b", "20"), fakeHandler.lastRequest.getQueryParams());
    }

    @Test
    void requestorPostBuildsExpectedRequest() {
        FakeClientRequestHandler fakeHandler = new FakeClientRequestHandler();
        Requestor requestor = new Requestor(fakeHandler);
        AbsoluteObjectReference reference = new AbsoluteObjectReference(
                "http",
                "localhost",
                8080,
                ObjectId.of("calculadora")
        );

        requestor.post(reference, "/body", "conteudo");

        assertEquals("POST", fakeHandler.lastRequest.getHttpMethod());
        assertEquals("/body", fakeHandler.lastRequest.getRemotePath());
        assertEquals("conteudo", fakeHandler.lastRequest.getBody());
    }

    @Test
    void remoteInvocationResponseIsSuccessReflectsStatusCode() {
        assertTrue(new RemoteInvocationResponse(200, "ok").isSuccess());
        assertTrue(!new RemoteInvocationResponse(404, "not found").isSuccess());
    }

    @Test
    void remoteInvocationRequestValidatesAndNormalizesInput() {
        AbsoluteObjectReference reference = new AbsoluteObjectReference(
                "http",
                "localhost",
                8080,
                ObjectId.of("calculadora")
        );

        RemoteInvocationRequest request = new RemoteInvocationRequest(
                reference,
                "get",
                "soma",
                null,
                null
        );

        assertEquals("GET", request.getHttpMethod());
        assertEquals("/soma", request.getRemotePath());
        assertEquals(Map.of(), request.getQueryParams());

        assertThrows(IllegalArgumentException.class, () -> new RemoteInvocationRequest(
                null, "GET", "/soma", Map.of(), null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RemoteInvocationRequest(
                reference, "   ", "/soma", Map.of(), null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RemoteInvocationRequest(
                reference, "GET", "   ", Map.of(), null
        ));
    }
}
