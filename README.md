# Rate Limiter

> Implementação de rate limiting com algoritmos clássicos em Java puro, exposta via API REST com Spring Boot.

---

## Sobre

Projeto de portfólio demonstrando domínio de **concorrência**, **estruturas de dados** e **design de algoritmos** — sem depender de frameworks para a lógica central.

A camada `core` é Java puro: nenhuma anotação Spring, nenhuma dependência de infraestrutura. O Spring Boot existe apenas para expor os algoritmos via HTTP.

---

## Algoritmos

### Token Bucket

Cada cliente possui um "balde" com capacidade máxima de tokens. Tokens são repostos continuamente a uma taxa fixa. Cada requisição consome 1 token — se o balde estiver vazio, a requisição é negada.

- ✅ Permite bursts controlados (até a capacidade do balde)
- ✅ Reposição contínua e suave
- ⚠️ Não garante distribuição uniforme dentro de uma janela fixa

### Sliding Window Counter

Mantém o timestamp de cada requisição por cliente. A cada nova requisição, descarta os timestamps fora da janela ativa e verifica se o total restante está dentro do limite.

- ✅ Distribuição uniforme — sem burst na virada de janela
- ✅ Controle preciso de taxa
- ⚠️ Maior uso de memória (guarda um timestamp por requisição)

---

## Stack

| Camada | Tecnologia |
|--------|-----------:|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.7 |
| Build | Maven |
| Testes | JUnit 5, Mockito, AssertJ |
| Documentação | Springdoc OpenAPI (Swagger UI) |
| Observabilidade | Spring Boot Actuator |
| Containerização | Docker e Docker Compose |

---

## Como rodar

Pré-requisitos: Java 21 e Docker.

```bash
docker-compose up -d
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

Endpoints documentados em `http://localhost:8080/swagger-ui.html`.

---

## API

### `POST /api/rate-limit/check`

Verifica se o cliente está dentro do limite.

**Request:**
```json
{
  "clientId": "usuario-123"
}
```

**Response 200 — permitido:**
```json
{
  "allowed": true,
  "remainingTokens": 9,
  "retryAfterMillis": 0,
  "algorithm": "TOKEN_BUCKET"
}
```

**Response 429 — bloqueado:**
```json
{
  "allowed": false,
  "remainingTokens": 0,
  "retryAfterMillis": 200,
  "algorithm": "TOKEN_BUCKET"
}
```

### `GET /api/rate-limit/status`

Retorna o algoritmo ativo.

```json
{
  "algorithm": "TOKEN_BUCKET",
  "status": "active"
}
```

### Outros

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check da aplicação |
| `GET /swagger-ui.html` | Documentação interativa |
| `GET /api-docs` | Especificação OpenAPI (JSON) |

---

## Testes

```bash
./mvnw verify
```

Cobertura por classe:

| Classe | O que é testado |
|--------|-----------------|
| `TokenBucketRateLimiterTest` | Limite de capacidade, negação, tokens restantes, isolamento por cliente, validações, **concorrência com 50 threads** |
| `SlidingWindowRateLimiterTest` | Limite de janela, retry-after, **janela deslizante em runtime**, isolamento por cliente, validações, **concorrência com 50 threads** |
| `RateLimitServiceTest` | Delegação ao algoritmo, propagação de negação, validação de entrada, trim de clientId |

---

## Decisões de design

- **RateLimiter como interface:** trocar o algoritmo ativo não exige mudança na camada de aplicação nem na API. Apenas a configuração muda.
- **Thread-safety com ReentrantLock:** granularidade fina por cliente — clientes diferentes nunca se bloqueiam entre si.
- **Core sem Spring:** a lógica de rate limiting é testável independentemente de HTTP e banco de dados.

---

Desenvolvido por

João Victor · [GitHub](https://github.com/nevvesdev) · [LinkedIn](https://www.linkedin.com/in/nevvesdev/)
