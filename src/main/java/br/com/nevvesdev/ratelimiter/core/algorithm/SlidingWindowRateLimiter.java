package br.com.nevvesdev.ratelimiter.core.algorithm;

import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sliding Window Counter — janela deslizante de tempo.
 *
 * Funcionamento:
 * - Mantém um registro dos timestamps de cada requisição por cliente.
 * - A cada nova requisição, descarta timestamps fora da janela de tempo.
 * - Se o total dentro da janela for menor que o limite, permite. Caso contrário, nega.
 *
 * Vantagem: distribuição uniforme — não sofre com o problema de burst
 * na virada de janela que o Fixed Window tem.
 *
 * Desvantagem: maior uso de memória (guarda timestamp de cada requisição).
 *
 * Estrutura de dados: ArrayDeque por cliente — O(1) para inserção no fim
 * e remoção no início (timestamps sempre chegam em ordem crescente).
 *
 * Thread-safety: ReentrantLock por cliente, mesma estratégia do Token Bucket.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final long windowSizeMillis;

    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowSizeMillis) {
        if (maxRequests <= 0) throw new IllegalArgumentException("Limite de requisições deve ser maior que zero");
        if (windowSizeMillis <= 0) throw new IllegalArgumentException("Tamanho da janela deve ser maior que zero");

        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId) {
        WindowState state = windows.computeIfAbsent(clientId, id -> new WindowState());
        return state.tryAcquire(maxRequests, windowSizeMillis);
    }

    @Override
    public String algorithmName() {
        return "SLIDING_WINDOW";
    }

    /**
     * Estado interno de um cliente.
     * A deque guarda os timestamps das requisições dentro da janela ativa.
     */
    private static class WindowState {

        private final ReentrantLock lock = new ReentrantLock();
        private final Deque<Long> timestamps = new ArrayDeque<>();

        RateLimitResult tryAcquire(int maxRequests, long windowSizeMillis) {
            lock.lock();
            try {
                long now = System.currentTimeMillis();
                long windowStart = now - windowSizeMillis;

                // Remove requisições fora da janela deslizante
                while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                    timestamps.pollFirst();
                }

                if (timestamps.size() < maxRequests) {
                    timestamps.addLast(now);
                    long remaining = maxRequests - timestamps.size();
                    return RateLimitResult.allowed(remaining);
                }

                // Tempo até o timestamp mais antigo sair da janela
                long oldestTimestamp = timestamps.peekFirst();
                long retryAfter = (oldestTimestamp + windowSizeMillis) - now;
                return RateLimitResult.denied(retryAfter);

            } finally {
                lock.unlock();
            }
        }
    }
}