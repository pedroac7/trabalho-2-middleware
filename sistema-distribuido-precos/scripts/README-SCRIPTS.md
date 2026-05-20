# Scripts de Apresentacao

Os scripts desta pasta foram simplificados para o fluxo oficial baseado em middleware.

## Como executar

Abra um terminal na raiz de `sistema-distribuido-precos` e rode:

```bat
scripts\start-http.bat
```

Este script sobe o fluxo middleware completo (gateway + validadores + repositorios).

Para encerrar todas as janelas abertas:

```bat
scripts\stop-all.bat
```

## O que o script start abre

- 1 gateway (`gateway.Main`)
- 2 validadores (`validador.Main`)
- 2 repositorios (`repositorio.Main`)

Todos os processos usam middleware para comunicacao de negocio, e heartbeat para discovery.

## Portas usadas

- Gateway service: `8080`
- Gateway heartbeat: `9090`
- Validador 1 service: `8081`
- Validador 2 service: `8084`
- Repositorio 1 service: `8082`
- Repositorio 2 service: `8083`

## Observacoes

- Os scripts assumem que os modulos ja foram compilados.
- Cada modulo precisa de `target/classes` e `target/dependency/*`.
- Protocolos HTTP/TCP/UDP antigos da aplicacao foram removidos; os plug-ins de protocolo permanecem no `middleware-core`.