package br.com.nevvesdev.ratelimiter.core.algorithm;

import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket — algoritmo clássico de rate limiting.
 *
 * Funcionamento:
 * - Cada cliente possui um "balde" com capacidade máxima de tokens.
 * - Tokens são repostos continuamente a uma taxa fixa (refillRatePerSecond).
 * - Cada requisição consome 1 token. Se o balde estiver vazio, a requisição é negada.
 *
 * Vantagem: permite bursts controlados (até a capacidade do balde).
 * Desvantagem: não garante distribuição uniforme dentro de uma janela.
 *
 * Thread-safety: ReentrantLock por cliente — evita condição de corrida
 * sem bloquear clientes diferentes entre si (granularidade fina).
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final long refillRatePerSecond;

    // Estado de cada cliente
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacidade deve ser maior que zero");
        if (refillRatePerSecond <= 0) throw new IllegalArgumentException("Taxa de reposição deve ser maior que zero");

        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId) {
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> new Bucket(capacity));
        return bucket.tryConsume(refillRatePerSecond, capacity);
    }

    @Override
    public String algorithmName() {
        return "TOKEN_BUCKET";
    }

    /**
     * Estado interno de um cliente.
     * Lock por instância garante que operações de reposição + consumo
     * sejam atômicas sem afetar outros clientes.
     */
    private static class Bucket {

        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicLong tokens;
        private volatile long lastRefillTimestamp;

        Bucket(long initialTokens) {
            this.tokens = new AtomicLong(initialTokens);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        RateLimitResult tryConsume(long refillRatePerSecond, long capacity) {
            lock.lock();
            try {
                refill(refillRatePerSecond, capacity);

                long current = tokens.get();
                if (current > 0) {
                    tokens.decrementAndGet();
                    return RateLimitResult.allowed(current - 1);
                }

                // Calcula quanto tempo até o próximo token
                long millisPerToken = 1000L / refillRatePerSecond;
                return RateLimitResult.denied(millisPerToken);

            } finally {
                lock.unlock();
            }
        }

        private void refill(long refillRatePerSecond, long capacity) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;

            if (elapsed <= 0) return;

            long tokensToAdd = (elapsed * refillRatePerSecond) / 1000L;
            if (tokensToAdd <= 0) return;

            long updated = Math.min(capacity, tokens.get() + tokensToAdd);
            tokens.set(updated);
            lastRefillTimestamp = now;
        }
    }
}