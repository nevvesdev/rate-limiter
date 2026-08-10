package br.com.nevvesdev.ratelimiter.infrastructure.web.dto;

import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;

public record RateLimitResponse(
        boolean allowed,
        long remainingTokens,
        long retryAfterMillis,
        String algorithm
) {
    public static RateLimitResponse from(RateLimitResult result, String algorithm) {
        return new RateLimitResponse(
                result.isAllowed(),
                result.getRemainingTokens(),
                result.getRetryAfterMillis(),
                algorithm
        );
    }
}