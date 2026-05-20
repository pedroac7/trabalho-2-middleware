package gateway.client;

import br.ufrn.middleware.client.RemoteInvocationResponse;
import br.ufrn.middleware.client.Requestor;
import br.ufrn.middleware.identification.AbsoluteObjectReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gateway.model.PrecoPayload;
import gateway.model.StorageClientResult;

import java.io.IOException;

public class MiddlewareRepositorioClient implements RepositorioClient {
    private static final String DEFAULT_COMPONENT_PATH = "/repositorio";

    private final Requestor requestor;
    private final ObjectMapper objectMapper;
    private final AbsoluteObjectReference fixedReference;

    public MiddlewareRepositorioClient() {
        this(new Requestor(), new ObjectMapper(), null);
    }

    public MiddlewareRepositorioClient(int timeoutMillis) {
        this(new Requestor(timeoutMillis), new ObjectMapper(), null);
    }

    public MiddlewareRepositorioClient(String baseUri) {
        this(new Requestor(), new ObjectMapper(), AbsoluteObjectReference.fromUri(baseUri));
    }

    public MiddlewareRepositorioClient(String baseUri, int timeoutMillis) {
        this(new Requestor(timeoutMillis), new ObjectMapper(), AbsoluteObjectReference.fromUri(baseUri));
    }

    MiddlewareRepositorioClient(Requestor requestor, ObjectMapper objectMapper) {
        this(requestor, objectMapper, null);
    }

    private MiddlewareRepositorioClient(
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
    public StorageClientResult armazenar(String host, int port, PrecoPayload preco) throws IOException {
        return armazenar("http", host, port, preco);
    }

    @Override
    public StorageClientResult armazenar(String protocol, String host, int port, PrecoPayload preco) throws IOException {
        try {
            AbsoluteObjectReference reference = resolveReference(protocol, host, port);
            String json = objectMapper.writeValueAsString(preco);
            RemoteInvocationResponse response = requestor.post(reference, "/armazenar", json);

            MiddlewareResponseEnvelope envelope;
            try {
                envelope = MiddlewareResponseEnvelope.fromJson(objectMapper, response.getBody());
            } catch (IOException e) {
                return StorageClientResult.error("RESPOSTA_INVALIDA_DO_REPOSITORIO");
            }

            boolean transportSuccess = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
            if (!transportSuccess || !envelope.success()) {
                return StorageClientResult.error(envelope.messageOrDefault("FALHA_AO_ARMAZENAR"));
            }

            if (envelope.result() == null || envelope.result().isNull()) {
                return StorageClientResult.error("RESPOSTA_INVALIDA_DO_REPOSITORIO");
            }

            try {
                return objectMapper.treeToValue(envelope.result(), StorageClientResult.class);
            } catch (IOException e) {
                return StorageClientResult.error("RESPOSTA_INVALIDA_DO_REPOSITORIO");
            }
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("FALHA_AO_CHAMAR_REPOSITORIO_VIA_MIDDLEWARE", e);
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
