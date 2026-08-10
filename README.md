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


**Decisão de design:** `RateLimiter` é uma interface — trocar o algoritmo ativo não exige mudança na camada de aplicação nem na API. Apenas a configuração muda.

**Thread-safety:** `ReentrantLock` por cliente com granularidade fina — clientes diferentes nunca se bloqueiam entre si.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 |
| Build | Maven |
| Testes | JUnit 5 + Mockito + AssertJ |
| Documentação | Springdoc OpenAPI (Swagger UI) |
| Observabilidade | Spring Boot Actuator |
| Containerização | Docker + Docker Compose |

---

## Endpoints

### `POST /api/rate-limit/check`

Verifica se o cliente está dentro do limite.

**Request:**
```json
{ "clientId": "usuario-123" }
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
  "algoritmo": "TOKEN_BUCKET",
  "status": "ativo"
}
```

### Outros

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check da aplicação |
| `GET /swagger-ui.html` | Documentação interativa |
| `GET /api-docs` | Especificação OpenAPI (JSON) |

---


Cobertura dos testes:

| Classe | O que é testado |
|--------|----------------|
| `TokenBucketRateLimiterTest` | Limite de capacidade, negação, tokens restantes, isolamento por cliente, validações, **concorrência com 50 threads** |
| `SlidingWindowRateLimiterTest` | Limite de janela, retry-after, **janela deslizante em runtime**, isolamento por cliente, validações, **concorrência com 50 threads** |
| `RateLimitServiceTest` | Delegação ao algoritmo, propagação de negação, validação de entrada, trim de clientId, algoritmo nulo |

---