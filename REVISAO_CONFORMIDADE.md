# Revisao de Conformidade - Segundo Trabalho Pratico

## Escopo da revisao
- Revisao estatica do repositorio, sem alterar codigo de producao neste passo.
- Sem execucao de testes neste relatorio; as evidencias abaixo sao por inspecao de estrutura, classes e testes existentes.

## 1) Separacao entre middleware e aplicacao

### Estrutura
- `middleware-core/` separado da aplicacao.
- `sistema-distribuido-precos/` separado por modulos:
  - `api-gateway/`
  - `validador-precos/`
  - `repositorio-precos/`

### Importacao do middleware-core como JAR Maven
- `sistema-distribuido-precos/api-gateway/pom.xml` importa `br.ufrn:middleware-core:1.0.0`.
- `sistema-distribuido-precos/validador-precos/pom.xml` importa `br.ufrn:middleware-core:1.0.0`.
- `sistema-distribuido-precos/repositorio-precos/pom.xml` importa `br.ufrn:middleware-core:1.0.0`.

Conclusao: separacao estrutural e dependencia Maven do middleware estao presentes.

---

## 2) Modelo de componentes

### Anotacoes existentes
- `@RemoteComponent` (`middleware-core/src/main/java/br/ufrn/middleware/annotations/RemoteComponent.java`)
- `@RemoteMethod` (`middleware-core/src/main/java/br/ufrn/middleware/annotations/RemoteMethod.java`)
- `@Param` (`middleware-core/src/main/java/br/ufrn/middleware/annotations/Param.java`)
- `@Body` (`middleware-core/src/main/java/br/ufrn/middleware/annotations/Body.java`)
- `@Lifecycle` (`middleware-core/src/main/java/br/ufrn/middleware/annotations/Lifecycle.java`)

### Classes de negocio anotadas
- `gateway.service.GatewayService`
  - `@RemoteComponent("gateway")`
  - `@Lifecycle(LifecycleType.STATIC_INSTANCE)`
  - metodo remoto: `@RemoteMethod(method = "POST", path = "/precos")`
  - payload: `@Body PrecoPayload`
- `validador.service.ValidadorService`
  - `@RemoteComponent("validador")`
  - `@Lifecycle(LifecycleType.STATIC_INSTANCE)`
  - metodo remoto: `@RemoteMethod(method = "POST", path = "/validar")`
  - payload: `@Body PrecoPayload`
- `repositorio.service.RepositorioService`
  - `@RemoteComponent("repositorio")`
  - `@Lifecycle(LifecycleType.STATIC_INSTANCE)`
  - metodo remoto: `@RemoteMethod(method = "POST", path = "/armazenar")`
  - payload: `@Body PrecoPayload`

Conclusao: o modelo preserva assinatura dos metodos de negocio e usa DTO via `@Body` para entrada JSON.

---

## 3) Basic Remoting Patterns

| Padrao | Classe(s) no middleware-core | Explicacao curta | Evidencia/teste |
|---|---|---|---|
| Broker | `br.ufrn.middleware.core.Broker` | Orquestra lookup, bind, invoke, interceptor e resposta interna. | `BrokerIntegrationTest` |
| Server Request Handler | `HttpProtocolPlugin`, `TcpProtocolPlugin`, `UdpProtocolPlugin` | Recebem requisicao de rede, convertem para `InvocationRequest`, chamam `Broker`. | `TcpProtocolPluginTest`, `UdpProtocolPluginTest`, `HttpBodyDtoIntegrationTest` |
| Client Request Handler | `ClientRequestHandler`, `HttpClientRequestHandler`, `TcpClientRequestHandler`, `UdpClientRequestHandler` | Encapsulam envio client-side por protocolo. | `RequestorTest`, `TcpClientRequestHandlerIntegrationTest`, `UdpProtocolPluginTest` |
| Requestor | `br.ufrn.middleware.client.Requestor` | Fachada client-side que escolhe handler por protocolo da `AbsoluteObjectReference`. | `RequestorTest`, `TcpClientRequestHandlerIntegrationTest` |
| Invoker | `br.ufrn.middleware.invoker.Invoker` | Invocacao reflexiva de metodo remoto com tratamento de erro. | `BrokerIntegrationTest` |
| Marshaller | `ResponseMarshaller`, `SimpleJsonResponseMarshaller`, `InvocationRequestMarshaller`, `SimpleTextInvocationRequestMarshaller`, `JsonBodyMarshaller` | Serializa resposta e (de)serializa mensagem de invocacao TCP/UDP e body JSON para DTO. | `SimpleJsonResponseMarshallerTest`, `SimpleTextInvocationRequestMarshallerTest`, `JsonBodyMarshallerTest` |
| Remote Object | `RemoteObjectRegistry`, `RemoteObjectDescriptor`, `RemoteMethodDescriptor` | Registro de componentes/metodos remotos e metadados de roteamento/lifecycle. | `BrokerIntegrationTest`, `IdentificationPatternsTest`, `LifecycleManagementTest` |
| Remoting Error | `RemotingException` + especializacoes (`BindingException`, `InvocationException`, `RemoteMethodNotFoundException`, `MarshallingException`) | Hierarquia de erros do middleware, propagada no fluxo. | `BrokerIntegrationTest`, testes de marshaller e protocolo |

Observacao: o padrao "Server Request Handler" esta materializado por plugin de protocolo (nao por pacote `handler` dedicado). Aderencia conceitual: OK; nomenclatura: verificar se o enunciado exige classe nomeada explicitamente.

---

## 4) Identification Patterns

| Padrao | Classe(s) | Uso no fluxo real |
|---|---|---|
| Lookup | `Lookup`, `LocalLookup`, `RemoteObjectRegistry.asLookup()`, `Middleware.getLookup()` | Busca local de componente/metodo no registro. |
| Object Id | `ObjectId`, campo em `RemoteObjectDescriptor` e `RemoteMethodDescriptor` | Identificacao canonica do componente remoto. |
| Absolute Object Reference | `AbsoluteObjectReference` | Requestor e clients internos do gateway constroem alvo remoto via URI (`http://.../validador`, `http://.../repositorio`, etc.). |

Evidencia: `IdentificationPatternsTest` + uso em `MiddlewareValidadorClient`, `MiddlewareRepositorioClient`, exemplos TCP/UDP/HTTP.

---

## 5) Lifecycle Management

| Tipo | Implementacao | Configuracao | Evidencia |
|---|---|---|---|
| STATIC_INSTANCE | provider fixo em `RemoteObjectRegistry.createTargetProvider` | `@Lifecycle(STATIC_INSTANCE)` ou default | `LifecycleManagementTest.staticInstanceMantemEstadoEntreChamadas` |
| PER_REQUEST_INSTANCE | nova instancia por invocacao | `@Lifecycle(PER_REQUEST_INSTANCE)` + `register(Class<?>)` | `LifecycleManagementTest.perRequestInstanceNaoMantemEstadoEntreChamadas` |
| LAZY_ACQUISITION | instancia cria sob demanda (thread-safe com `AtomicReference` + lock) | `@Lifecycle(LAZY_ACQUISITION)` + `register(Class<?>)` | `AdvancedLifecycleManagementTest.lazyAcquisition*` |
| POOLING | pool fixo (default 5), selecao round-robin com `AtomicInteger` | `@Lifecycle(POOLING)` + `register(Class<?>)` | `AdvancedLifecycleManagementTest.pooling*` |

Nao implementados (explicitamente):
- `CLIENT_DEPENDENT_INSTANCE`
- `LEASING`
- `PASSIVATION`

Comportamento atual para os nao implementados: `IllegalArgumentException` em registro por classe (`AdvancedLifecycleManagementTest.unsupportedLifecycleThrowsClearException`).

---

## 6) Extension Patterns

| Padrao | Classe(s) | Evidencia |
|---|---|---|
| Invocation Interceptor | `InvocationInterceptor`, `LoggingInterceptor`, suporte no `Broker` | `InvocationInterceptorTest` |
| Invocation Context | `InvocationContext`, integracao no `Broker` | `InvocationContextPropagationTest` |
| Protocol Plug-In | `ProtocolPlugin`, `HttpProtocolPlugin`, `TcpProtocolPlugin`, `UdpProtocolPlugin` | `TcpProtocolPluginTest`, `UdpProtocolPluginTest`, `HttpBodyDtoIntegrationTest` |

---

## 7) Aplicacao integrada (estado atual)

Fluxo middleware atual (novo caminho):
1. `validador.Main` sobe `Middleware + ValidadorService + HttpProtocolPlugin(8081)` e inicia heartbeat.
2. `repositorio.Main` sobe `Middleware + RepositorioService + HttpProtocolPlugin(8082)` e inicia heartbeat.
3. `gateway.Main` sobe `HeartbeatReceiver`, cria clients internos middleware (`MiddlewareValidadorClient`/`MiddlewareRepositorioClient`), registra `GatewayService` no middleware e expoe HTTP em `http://localhost:8080/gateway`.
4. Requisicao de negocio: `POST /gateway/precos`.
5. `GatewayService` descobre endpoints por heartbeat, valida no validador e replica no repositorio via clients internos que usam `Requestor`.

Heartbeat:
- Receiver no gateway: `heartbeat.HeartbeatReceiver`.
- Sender nos modulos downstream: `heartbeat.HeartbeatSender`.
- Tipo anunciado suporta sufixo de protocolo (`tipoEntidade|middleware`) para preservar compatibilidade com discovery atual.

---

## 8) Testes existentes

### middleware-core
- `br/ufrn/middleware/core/BrokerIntegrationTest.java`
- `br/ufrn/middleware/binding/ParameterBinderBodyDtoTest.java`
- `br/ufrn/middleware/marshaller/JsonBodyMarshallerTest.java`
- `br/ufrn/middleware/marshaller/SimpleJsonResponseMarshallerTest.java`
- `br/ufrn/middleware/marshaller/SimpleJsonResponseMarshallerObjectTest.java`
- `br/ufrn/middleware/marshaller/SimpleTextInvocationRequestMarshallerTest.java`
- `br/ufrn/middleware/identification/IdentificationPatternsTest.java`
- `br/ufrn/middleware/lifecycle/LifecycleManagementTest.java`
- `br/ufrn/middleware/lifecycle/AdvancedLifecycleManagementTest.java`
- `br/ufrn/middleware/interceptor/InvocationInterceptorTest.java`
- `br/ufrn/middleware/interceptor/InvocationContextPropagationTest.java`
- `br/ufrn/middleware/client/RequestorTest.java`
- `br/ufrn/middleware/client/TcpClientRequestHandlerIntegrationTest.java`
- `br/ufrn/middleware/protocol/TcpProtocolPluginTest.java`
- `br/ufrn/middleware/protocol/UdpProtocolPluginTest.java`
- `br/ufrn/middleware/protocol/HttpBodyDtoIntegrationTest.java`

### api-gateway
- `gateway/client/MiddlewareValidadorClientTest.java`
- `gateway/client/MiddlewareRepositorioClientTest.java`
- `gateway/GatewayMiddlewareIntegrationTest.java`

### validador-precos
- `validador/ValidadorMiddlewareIntegrationTest.java`

### repositorio-precos
- `repositorio/RepositorioMiddlewareIntegrationTest.java`

---

## 9) Testes manuais (fluxo completo)

Preferencia: executar em Git Bash para evitar problemas de quoting do `curl` no PowerShell.

### Terminal 1 - Validador
```bash
cd sistema-distribuido-precos/validador-precos
mvn exec:java "-Dexec.mainClass=validador.Main"
```

### Terminal 2 - Repositorio
```bash
cd sistema-distribuido-precos/repositorio-precos
mvn exec:java "-Dexec.mainClass=repositorio.Main"
```

### Terminal 3 - Gateway via middleware
```bash
cd sistema-distribuido-precos/api-gateway
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" gateway.Main 8080 9090 100 http middleware
```

### Terminal 4 - Requisicao valida
```bash
curl -X POST "http://localhost:8080/gateway/precos" \
  -H "Content-Type: application/json" \
  --data-raw '{"ativo":"PETR4","valor":35.50,"timestamp":1710000000000}'
```

### Terminal 4 - Requisicao invalida (erro de negocio)
```bash
curl -X POST "http://localhost:8080/gateway/precos" \
  -H "Content-Type: application/json" \
  --data-raw '{"ativo":"PETR4","valor":0,"timestamp":1710000000000}'
```

Esperado no invalido: envelope do middleware com `"success":true` e `result` contendo erro de negocio (ex.: `statusCode` 400 e mensagem de validacao).

---

## 10) JMeter (pendente)

### Endpoint-alvo
- `POST /gateway/precos`
- URL base sugerida: `http://localhost:8080/gateway/precos`

### Payload JSON
```json
{"ativo":"PETR4","valor":35.50,"timestamp":1710000000000}
```

### Header
- `Content-Type: application/json`

### Plano de carga sugerido
- 1 thread
- 5 threads
- 10 threads
- 25 threads
- 50 threads
- 100 threads
- 200 threads

### Metricas a coletar
- Throughput (req/s)
- Tempo medio
- p90/p95
- Percentual de erro

### Como identificar Knee Capacity e Usable Capacity
- Knee Capacity: ponto em que aumento de carga deixa de produzir ganho proporcional de throughput e a latencia acelera.
- Usable Capacity: carga imediatamente anterior ao knee, com latencia/erro ainda estaveis para operacao.

Status: execucao JMeter ainda pendente nesta revisao.

---

## 11) Lacunas e riscos reais

1. Java version entre modulos
- `middleware-core` compila em Java 17.
- Modulos da aplicacao (`api-gateway`, `validador-precos`, `repositorio-precos`) estao com `maven.compiler.source/target` em 21.
- Risco: divergencia com regra de Java 17 do enunciado; verificar criterio formal de entrega.

2. gRPC fora do escopo atual
- A remocao de gRPC foi uma decisao de escopo para simplificar a entrega baseada em middleware.
- Risco baixo: garantir apenas que scripts e documentos usados na apresentacao sigam o fluxo oficial `gateway.Main` + `validador.Main` + `repositorio.Main`.

3. Quoting de curl no Windows PowerShell
- Risco de falha de parsing de JSON em linha de comando.
- Mitigacao: usar Git Bash (ou ajustar quoting no PowerShell).

4. Possivel lock em `target/` no Windows durante `mvn clean`
- Risco intermitente por processo Java ainda ativo.
- Mitigacao: encerrar processos em portas de teste antes do clean.

5. JMeter ainda nao executado nesta revisao
- Risco: falta de evidencias de capacidade (knee/usable) ate a campanha de carga.

6. Lifecycle parcial por design
- `CLIENT_DEPENDENT_INSTANCE`, `LEASING`, `PASSIVATION` estao declarados no enum mas nao implementados no registry.
- Risco: expectativa de cobertura completa de lifecycle, se o avaliador exigir todos os tipos.

---

## Checklist de aprovacao

- [x] Middleware e aplicacao separados em modulos distintos.
- [x] Aplicacao importa `middleware-core` como dependencia Maven.
- [x] Modelo de componentes com anotacoes runtime (`@RemoteComponent`, `@RemoteMethod`, `@Param`, `@Body`, `@Lifecycle`).
- [x] `GatewayService`, `ValidadorService` e `RepositorioService` anotados como componentes remotos.
- [x] Basic Remoting Patterns mapeados no middleware-core.
- [x] Identification Patterns mapeados (`Lookup`, `ObjectId`, `AbsoluteObjectReference`).
- [x] Lifecycle implementado para `STATIC_INSTANCE`, `PER_REQUEST_INSTANCE`, `LAZY_ACQUISITION`, `POOLING`.
- [x] Extension Patterns mapeados (`InvocationContext`, `InvocationInterceptor`, `ProtocolPlugin`).
- [x] Plugins de protocolo listados: HTTP, TCP, UDP.
- [x] Fluxo integrado da aplicacao documentado (`validador.Main`, `repositorio.Main`, `gateway.Main`, heartbeat, `POST /gateway/precos`).
- [x] Testes existentes listados por modulo.
- [x] Comandos de teste manual (valido/invalido) documentados.
- [ ] Campanha JMeter executada e resultados anexados.
- [ ] Confirmacao final de conformidade de versao Java (17 vs 21) conforme criterio do avaliador.

