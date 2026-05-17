# AGENTS.md

## Contexto do projeto

Este repositório contém o segundo trabalho da disciplina de Programação Distribuída.

O objetivo é desenvolver uma plataforma de middleware em Java e adaptar a aplicação da primeira unidade para usar esse middleware via JAR.

A aplicação original é um sistema distribuído para processamento e replicação de preços financeiros, composto por:

- `api-gateway`
- `validador-precos`
- `repositorio-precos`

A nova entrega deve separar claramente:

- a plataforma de middleware;
- a aplicação que utiliza essa plataforma.

## Estrutura do repositório

```text
TRABALHO-2-MIDDLEWARE/
  middleware-core/
  sistema-distribuido-precos/
    api-gateway/
    validador-precos/
    repositorio-precos/
    jmeter/
```

## Papel de cada pasta

### `middleware-core/`

Projeto Maven que implementa a plataforma de middleware.

Esse projeto deve gerar um `.jar`.

O middleware deve conter as responsabilidades de comunicação distribuída, invocação remota, registro de objetos remotos, serialização, tratamento de erros, ciclo de vida, interceptadores e plug-ins de protocolo.

### `sistema-distribuido-precos/`

Aplicação da primeira unidade.

Essa aplicação será adaptada para importar o `.jar` do middleware e expor suas classes de negócio por meio de anotações.

Ela não deve conter a infraestrutura principal de comunicação remota depois da refatoração.

## Regras importantes

- Não misturar código do middleware dentro dos módulos da aplicação.
- Não misturar regras de negócio da aplicação dentro do middleware.
- O middleware deve ser uma biblioteca Java empacotada como JAR.
- A aplicação deve importar o JAR do middleware.
- Começar pelo MVP HTTP.
- Mesmo no MVP HTTP, manter a arquitetura preparada para Protocol Plug-In.
- Usar Java 17.
- Usar Maven.
- Evitar frameworks externos como Spring.
- Priorizar código simples, didático e explícito.
- Preferir classes pequenas e com responsabilidade clara.
- Não implementar tudo de uma vez.
- Fazer mudanças pequenas, compiláveis e fáceis de revisar.

## Padrões que devem aparecer no middleware

A plataforma de middleware deve evidenciar os seguintes padrões/conceitos:

- Broker
- Server Request Handler
- Client Request Handler
- Requestor
- Invoker
- Marshaller
- Remote Object
- Remoting Error
- Lookup
- Object Id
- Absolute Object Reference
- Lifecycle Management
- Invocation Interceptor
- Invocation Context
- Protocol Plug-In

## Estratégia de implementação

O desenvolvimento deve seguir esta ordem:

1. Criar anotações do modelo de componentes.
2. Criar o registro de objetos remotos.
3. Criar descritores de objetos e métodos remotos.
4. Criar o Invoker usando reflexão.
5. Criar um Marshaller simples para JSON/string.
6. Criar o HttpProtocolPlugin.
7. Criar um exemplo mínimo com CalculadoraService.
8. Adicionar lifecycle: Static Instance e Per-Request Instance.
9. Adicionar Remoting Error.
10. Adicionar Invocation Context e Invocation Interceptor.
11. Adicionar Requestor/Client Request Handler.
12. Adaptar `validador-precos`.
13. Adaptar `repositorio-precos`.
14. Adaptar `api-gateway`.
15. Adicionar segundo protocolo, preferencialmente TCP.
16. Preparar testes com JMeter.

## MVP esperado

O primeiro MVP do middleware deve permitir registrar uma classe assim:

```java
@RemoteComponent("calculadora")
public class CalculadoraService {

    @RemoteMethod(method = "GET", path = "/soma")
    public int soma(@Param("a") int a, @Param("b") int b) {
        return a + b;
    }
}
```

E subir um servidor assim:

```java
public class Main {
    public static void main(String[] args) {
        Middleware middleware = new Middleware();

        middleware.register(new CalculadoraService());

        middleware
            .useProtocol(new HttpProtocolPlugin(8080))
            .start();
    }
}
```

A chamada esperada no MVP:

```text
GET /calculadora/soma?a=10&b=20
```

Resposta esperada:

```json
{
  "result": 30
}
```

## Modelo de componentes

As classes de negócio devem ser descritas por anotações.

Exemplos esperados:

```java
@RemoteComponent("validador")
public class ValidadorService {

    @RemoteMethod(method = "POST", path = "/validar")
    public ValidacaoResponse validar(@Body PrecoRequest request) {
        // regra de negócio
    }
}
```

```java
@RemoteComponent("repositorio")
@Lifecycle(LifecycleType.STATIC_INSTANCE)
public class RepositorioService {

    @RemoteMethod(method = "POST", path = "/precos")
    public RepositorioResponse armazenar(@Body PrecoValidado preco) {
        // regra de negócio
    }
}
```

As anotações devem ter retenção em tempo de execução:

```java
@Retention(RetentionPolicy.RUNTIME)
```

## Convenções de pacote

Usar o pacote base:

```text
br.ufrn.middleware
```

Subpacotes recomendados:

```text
br.ufrn.middleware.annotations
br.ufrn.middleware.core
br.ufrn.middleware.protocol
br.ufrn.middleware.handler
br.ufrn.middleware.marshaller
br.ufrn.middleware.invoker
br.ufrn.middleware.registry
br.ufrn.middleware.lifecycle
br.ufrn.middleware.interceptor
br.ufrn.middleware.error
```

## Comandos úteis

Para compilar o middleware:

```bash
cd middleware-core
mvn clean package
```

Para compilar um módulo da aplicação:

```bash
cd sistema-distribuido-precos/api-gateway
mvn clean package
```

```bash
cd sistema-distribuido-precos/validador-precos
mvn clean package
```

```bash
cd sistema-distribuido-precos/repositorio-precos
mvn clean package
```

## Restrições

- Não usar Spring.
- Não usar frameworks HTTP externos no MVP.
- Não adicionar dependências sem necessidade.
- Não alterar a aplicação antiga antes do middleware MVP funcionar.
- Não remover código antigo da aplicação sem uma etapa explícita de refatoração.
- Não implementar TCP/UDP antes do fluxo HTTP mínimo funcionar.
- Não gerar código excessivamente genérico ou complexo.

## Critério de qualidade

Antes de concluir uma tarefa, verificar:

- O projeto compila com `mvn clean package`.
- As classes estão no pacote correto.
- O código tem nomes alinhados aos padrões do trabalho.
- A mudança é pequena e revisável.
- A separação entre middleware e aplicação foi preservada.
- Não foram adicionados arquivos gerados como `target/`, `.class` ou logs.

## Estilo de código

- Código Java simples.
- Nomes claros.
- Métodos curtos.
- Baixo acoplamento.
- Sem overengineering.
- Comentários apenas quando ajudarem a explicar uma decisão arquitetural ou padrão de middleware.

## Como agir ao receber uma tarefa

Ao receber uma tarefa de implementação:

1. Ler este arquivo.
2. Identificar a pasta correta.
3. Alterar apenas o necessário.
4. Manter o projeto compilável.
5. Informar quais arquivos foram criados ou modificados.
6. Sugerir o comando de validação.
