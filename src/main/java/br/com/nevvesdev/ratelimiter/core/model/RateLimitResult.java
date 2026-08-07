package br.com.nevvesdev.ratelimiter.core.model;

/**
 Representa o resultado de uma verificação de rate limit.
 Imutável por design — criado via métodos de fábrica estáticos.
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final long remainingTokens;
    private final long retryAfterMillis;

    private RateLimitResult(boolean allowed, long remainingTokens, long retryAfterMillis) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.retryAfterMillis = retryAfterMillis;
    }

    public static RateLimitResult allowed(long remainingTokens) {
        return new RateLimitResult(true, remainingTokens, 0);
    }

    public static RateLimitResult denied(long retryAfterMillis) {
        return new RateLimitResult(false, 0, retryAfterMillis);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    @Override
    public String toString() {
        return "RateLimitResult{" +
                "allowed=" + allowed +
                ", remainingTokens=" + remainingTokens +
                ", retryAfterMillis=" + retryAfterMillis +
                '}';
    }
}