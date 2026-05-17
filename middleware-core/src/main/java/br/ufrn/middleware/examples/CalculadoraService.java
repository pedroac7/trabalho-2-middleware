package br.ufrn.middleware.examples;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Param;
import br.ufrn.middleware.annotations.RemoteComponent;
import br.ufrn.middleware.annotations.RemoteMethod;

@RemoteComponent("calculadora")
public class CalculadoraService {

    @RemoteMethod(method = "GET", path = "/soma")
    public int soma(@Param("a") int a, @Param("b") int b) {
        return a + b;
    }

    @RemoteMethod(method = "GET", path = "/eco")
    public String eco(@Param("valor") String valor) {
        return valor;
    }

    @RemoteMethod(method = "POST", path = "/body")
    public String body(@Body String body) {
        return body;
    }
}
