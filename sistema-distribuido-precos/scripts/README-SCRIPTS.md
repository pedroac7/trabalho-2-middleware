# Scripts de Apresentacao

Os scripts desta pasta sobem o fluxo oficial baseado no `middleware-core`.

## Como executar

Abra um terminal na raiz de `sistema-distribuido-precos` e rode um dos scripts:

```bat
scripts\start-http.bat
scripts\start-tcp.bat
scripts\start-udp.bat
```

Para encerrar as janelas abertas:

```bat
scripts\stop-all.bat
```

## Execucao JMeter (opcional)

```bat
scripts\run-jmeter-http.bat [threads] [rampup] [loops] [host] [port]
scripts\run-jmeter-tcp.bat [threads] [rampup] [loops] [host] [port]
scripts\run-jmeter-udp.bat [threads] [rampup] [loops] [host] [port] [timeoutMs]
```

## O que cada script abre

- 1 gateway (`gateway.Main`)
- 2 validadores (`validador.Main`)
- 2 repositorios (`repositorio.Main`)

Todos usam middleware para comunicacao de negocio. O heartbeat continua ativo para discovery.

## Portas usadas

- Gateway service: `8080`
- Gateway heartbeat: `9090`
- Validador 1 service: `8081`
- Validador 2 service: `8084`
- Repositorio 1 service: `8082`
- Repositorio 2 service: `8083`

## Testes manuais

- HTTP: `curl` em `POST http://localhost:8080/gateway/precos`.
- TCP: `nc`/`ncat` com payload textual do middleware em `localhost:8080`.
- UDP: `nc -u`/`ncat -u` com payload textual do middleware em `localhost:8080`.
