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

## Como testar o cliente Requestor

Terminal 1:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraServer"
```

Terminal 2:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraClient"
```

## Como testar o protocolo TCP

Terminal 1:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraTcpServer"
```

Terminal 2:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraTcpClient"
```

O servidor usa `TcpProtocolPlugin`.
O cliente usa `Requestor` + `TcpClientRequestHandler`.
A seleÃ§Ã£o do handler no cliente Ã© feita pelo protocolo da `AbsoluteObjectReference`, por exemplo `tcp://localhost:9090/calculadora`.

## Como testar o protocolo UDP

Terminal 1:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraUdpServer"
```

Terminal 2:

```bash
mvn exec:java "-Dexec.mainClass=br.ufrn.middleware.examples.CalculadoraUdpClient"
```

O servidor usa `UdpProtocolPlugin`.
O cliente usa `Requestor` + `UdpClientRequestHandler`.
A seleção do handler no cliente é feita pelo protocolo da `AbsoluteObjectReference`, por exemplo `udp://localhost:9091/calculadora`.
UDP usa datagramas, então não há conexão persistente.

## Marshaller

- `ResponseMarshaller` serializa `InvocationResponse` para JSON textual.
- `InvocationRequestMarshaller` serializa e desserializa mensagens de invocação TCP/UDP.
- HTTP mantém parsing próprio de requisição por causa do formato HTTP, mas converte tudo para `InvocationRequest`.
- Todos os protocolos convergem para `InvocationRequest` e `InvocationResponse`.

## Invocation Context

- Cada requisição remota possui um `requestId`.
- O `Requestor` cria `requestId` automaticamente quando não é informado.
- HTTP transporta `requestId` no header `X-Request-Id`.
- TCP e UDP transportam `requestId` no campo `REQUEST_ID` do payload textual.
- Interceptors usam o mesmo `requestId` para logs e correlação ponta a ponta.
