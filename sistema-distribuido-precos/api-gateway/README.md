# api-gateway

## Executando gateway via middleware

Terminal 1:

```bash
cd sistema-distribuido-precos/validador-precos
mvn exec:java "-Dexec.mainClass=validador.ValidadorMiddlewareMain"
```

Terminal 2:

```bash
cd sistema-distribuido-precos/repositorio-precos
mvn exec:java "-Dexec.mainClass=repositorio.RepositorioMiddlewareMain"
```

Terminal 3:

```bash
cd sistema-distribuido-precos/api-gateway
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" gateway.GatewayMiddlewareMain 8080 9090 100 http middleware
```

Terminal 4:

```bash
curl -X POST "http://localhost:8080/gateway/precos" \
  -H "Content-Type: application/json" \
  --data-raw '{"ativo":"PETR4","valor":35.50,"timestamp":1710000000000}'
```

## Executando fluxo antigo (Main tradicional)

Terminal 1:

```bash
cd sistema-distribuido-precos/validador-precos
mvn exec:java "-Dexec.mainClass=validador.ValidadorMiddlewareMain"
```

Terminal 2:

```bash
cd sistema-distribuido-precos/repositorio-precos
mvn exec:java "-Dexec.mainClass=repositorio.RepositorioMiddlewareMain"
```

Terminal 3:

```bash
cd sistema-distribuido-precos/api-gateway
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" gateway.Main 8080 9090 http 100 http middleware
```

Terminal 4:

```bash
curl -X POST "http://localhost:8080/precos" \
  -H "Content-Type: application/json" \
  --data-raw '{"ativo":"PETR4","valor":35.50,"timestamp":1710000000000}'
```
