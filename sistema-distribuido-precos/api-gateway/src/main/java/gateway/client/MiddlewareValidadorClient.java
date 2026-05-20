package gateway.client;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.model.PrecoPayload;
import gateway.model.ValidationClientResult;

import java.io.IOException;

public class MiddlewareValidadorClient implements ValidadorClient {
    private static final String DEFAULT_COMPONENT_PATH = "/validador";

    private final Requestor requestor;
    private final ObjectMapper objectMapper;
    private final AbsoluteObjectReference fixedReference;

    public MiddlewareValidadorClient() {
        this(new Requestor(), new ObjectMapper(), null);
    }

    public MiddlewareValidadorClient(int timeoutMillis) {
        this(new Requestor(timeoutMillis), new ObjectMapper(), null);
    }

    public MiddlewareValidadorClient(String baseUri) {
        this(new Requestor(), new ObjectMapper(), AbsoluteObjectReference.fromUri(baseUri));
    }

    public MiddlewareValidadorClient(String baseUri, int timeoutMillis) {
        this(new Requestor(timeoutMillis), new ObjectMapper(), AbsoluteObjectReference.fromUri(baseUri));
    }

    MiddlewareValidadorClient(Requestor requestor, ObjectMapper objectMapper) {
        this(requestor, objectMapper, null);
    }

    private MiddlewareValidadorClient(
            Requestor requestor,
            ObjectMapper objectMapper,
            AbsoluteObjectReference fixedReference
    ) {
        if (requestor == null) {
            throw new IllegalArgumentException("Requestor must not be null.");
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        this.requestor = requestor;
        this.objectMapper = objectMapper;
        this.fixedReference = fixedReference;
    }

    @Override
    public ValidationClientResult validar(String host, int port, PrecoPayload preco) throws IOException {
        return validar("http", host, port, preco);
    }

    @Override
    public ValidationClientResult validar(String protocol, String host, int port, PrecoPayload preco) throws IOException {
        try {
            AbsoluteObjectReference reference = resolveReference(protocol, host, port);
            String json = objectMapper.writeValueAsString(preco);
            RemoteInvocationResponse response = requestor.post(reference, "/validar", json);
            MiddlewareResponseEnvelope envelope = MiddlewareResponseEnvelope.fromJson(objectMapper, response.getBody());

            boolean transportSuccess = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
            if (!transportSuccess || !envelope.success()) {
                String message = envelope.messageOrDefault("FALHA_AO_VALIDAR");
                if (response.getStatusCode() == 400 || envelope.statusCode() == 400) {
                    return ValidationClientResult.invalid(message);
                }
                throw new IOException(message);
            }

            if (envelope.result() == null || envelope.result().isNull()) {
                throw new IOException("RESPOSTA_INVALIDA_DO_VALIDADOR");
            }
            return objectMapper.treeToValue(envelope.result(), ValidationClientResult.class);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("FALHA_AO_CHAMAR_VALIDADOR_VIA_MIDDLEWARE", e);
        }
    }

    private AbsoluteObjectReference resolveReference(String protocol, String host, int port) {
        if (fixedReference != null) {
            return fixedReference;
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Protocol must not be null or blank.");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host must not be null or blank.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        return AbsoluteObjectReference.fromUri(protocol.trim().toLowerCase() + "://" + host + ":" + port + DEFAULT_COMPONENT_PATH);
    }
}
