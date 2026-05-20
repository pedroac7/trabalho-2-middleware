# api-gateway

## Fluxo oficial (middleware)

Terminal 1:

```bash
cd sistema-distribuido-precos/validador-precos
mvn exec:java "-Dexec.mainClass=validador.Main"
```

Terminal 2:

```bash
cd sistema-distribuido-precos/repositorio-precos
mvn exec:java "-Dexec.mainClass=repositorio.Main"
```

Terminal 3:

```bash
cd sistema-distribuido-precos/api-gateway
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" gateway.Main 8080 9090 100 http middleware
```

Terminal 4:

```bash
curl -X POST "http://localhost:8080/gateway/precos" \\
  -H "Content-Type: application/json" \\
  --data-raw '{"ativo":"PETR4","valor":35.50,"timestamp":1710000000000}'
```

Endpoint oficial:

- `POST http://localhost:8080/gateway/precos`

Observacoes:

- O gateway usa `MiddlewareValidadorClient` e `MiddlewareRepositorioClient` internamente.
- Heartbeat permanece ativo para discovery de validadores e repositorios.
- Protocolos HTTP/TCP/UDP da aplicacao foram removidos; os protocolos permanecem no `middleware-core` como plug-ins.