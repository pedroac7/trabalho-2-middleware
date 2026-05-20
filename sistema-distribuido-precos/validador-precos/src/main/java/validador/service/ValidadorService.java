package validador.service;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Lifecycle;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;
import br.ufrn.middleware.lifecycle.LifecycleType;
import validador.model.*;

@RemoteComponent("validador")
@Lifecycle(LifecycleType.STATIC_INSTANCE)
public class ValidadorService {

    private static final long PROCESSING_DELAY_MILLIS = 50;

    @RemoteMethod(method = "POST", path = "/validar")
    public ValidationResult validar(@Body PrecoPayload preco) {
        simularCustoDeProcessamento();

        if (preco == null) {
            return ValidationResult.invalid("JSON_INVALIDO");
        }
        if (preco.ativo() == null || preco.ativo().isBlank()) {
            return ValidationResult.invalid("ATIVO_OBRIGATORIO");
        }
        if (preco.valor() <= 0) {
            return ValidationResult.invalid("VALOR_INVALIDO");
        }
        if (preco.timestamp() <= 0) {
            return ValidationResult.invalid("TIMESTAMP_INVALIDO");
        }
        return ValidationResult.accepted();
    }

    private void simularCustoDeProcessamento() {
        try {
            Thread.sleep(PROCESSING_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
