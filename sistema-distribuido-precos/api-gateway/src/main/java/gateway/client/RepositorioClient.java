package gateway.client;

import gateway.model.*;

import java.io.IOException;

public interface RepositorioClient {
    StorageClientResult armazenar(String host, int port, PrecoPayload preco) throws IOException;
}
