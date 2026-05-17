# middleware-core

## Como compilar

```bash
mvn clean package
```

## Como iniciar exemplo

```bash
mvn exec:java -Dexec.mainClass="br.ufrn.middleware.examples.CalculadoraServer"
```

## Como testar

```bash
curl "http://localhost:8080/calculadora/soma?a=10&b=20"
curl "http://localhost:8080/calculadora/eco?valor=teste"
curl -X POST "http://localhost:8080/calculadora/body" -d "conteudo"
```
