package repositorio.service;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Lifecycle;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.lifecycle.LifecycleType;
import repositorio.model.*;

import java.util.concurrent.ConcurrentHashMap;

@RemoteComponent("repositorio")
@Lifecycle(LifecycleType.STATIC_INSTANCE)
public class RepositorioService {
    private final ConcurrentHashMap<String, PrecoArmazenado> precos = new ConcurrentHashMap<>();

    @RemoteMethod(method = "POST", path = "/armazenar")
    public StorageResult armazenar(@Body PrecoPayload preco) {
        if (preco == null) {
            return StorageResult.error("JSON_INVALIDO");
        }
        if (preco.ativo() == null || preco.ativo().isBlank()) {
            return StorageResult.error("ATIVO_OBRIGATORIO");
        }
        if (preco.valor() <= 0) {
            return StorageResult.error("VALOR_INVALIDO");
        }
        if (preco.timestamp() <= 0) {
            return StorageResult.error("TIMESTAMP_INVALIDO");
        }

        PrecoArmazenado precoArmazenado = new PrecoArmazenado(preco.ativo(), preco.valor(), preco.timestamp());
        precos.put(preco.ativo(), precoArmazenado);
        return StorageResult.success("ARMAZENADO");
    }
}
