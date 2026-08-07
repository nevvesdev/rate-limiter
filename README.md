# rate-limiter-java

> Implementação de rate limiting com algoritmos clássicos — portfólio técnico

---

## Sobre

Projeto demonstrando domínio de **concorrência**, **estruturas de dados** e
**design de algoritmos** em Java puro, exposto via API REST com Spring Boot.

Algoritmos implementados:
- **Token Bucket** — controle de burst com reposição contínua de tokens
- **Sliding Window Counter** — janela deslizante para controle preciso de taxa

---

## Stack

- Java 21
- Spring Boot 3.3
- Maven
- JUnit 5 + Mockito
- Springdoc OpenAPI (Swagger UI)
- Docker

--- 

## Fases

| Fase | Descrição                      | Status |
|------|--------------------------------|--------|
| 0    | Scaffold, estrutura, pom.xml   | ✅     |
| 1    | Interface RateLimiter + modelo | 🔜     |
| 2    | Token Bucket + testes          | 🔜     |
| 3    | Sliding Window + testes        | 🔜     |
| 4    | Application layer + testes     | 🔜     |
| 5    | API REST Spring Boot           | 🔜     |
| 6    | Docker + Actuator              | 🔜     |
| 7    | README profissional            | 🔜     |

---