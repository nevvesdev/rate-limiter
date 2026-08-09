package br.com.nevvesdev.ratelimiter.application;

import br.com.nevvesdev.ratelimiter.core.algorithm.RateLimiter;
import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;

import java.util.Objects;

/**
 * Camada de aplicação — orquestra qual algoritmo de rate limiting será usado.
 *
 * Não conhece Spring, não conhece HTTP.
 * Recebe um RateLimiter via construtor (injeção por interface) —
 * trocar o algoritmo não exige mudança aqui.
 *
 * Responsabilidades:
 * - Validar entrada
 * - Delegar ao algoritmo correto
 * - Centralizar logging futuro, métricas, auditoria
 */
public class RateLimitService {

    private final RateLimiter rateLimiter;

    public RateLimitService(RateLimiter rateLimiter) {
        Objects.requireNonNull(rateLimiter, "RateLimiter não pode ser nulo");
        this.rateLimiter = rateLimiter;
    }

    /**
     * Verifica se o cliente identificado por {@code clientId} está dentro do limite.
     *
     * @param clientId identificador do cliente — não pode ser nulo ou vazio
     * @return resultado da verificação
     */
    public RateLimitResult checkLimit(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId não pode ser nulo ou vazio");
        }
        return rateLimiter.tryAcquire(clientId.trim());
    }

    /**
     * Retorna o nome do algoritmo ativo — útil para a API informar ao cliente
     * qual estratégia está em uso.
     */
    public String activeAlgorithm() {
        return rateLimiter.algorithmName();
    }
}