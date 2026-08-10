package br.com.nevvesdev.ratelimiter.infrastructure.config;

import br.com.nevvesdev.ratelimiter.application.RateLimitService;
import br.com.nevvesdev.ratelimiter.core.algorithm.RateLimiter;
import br.com.nevvesdev.ratelimiter.core.algorithm.SlidingWindowRateLimiter;
import br.com.nevvesdev.ratelimiter.core.algorithm.TokenBucketRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central do rate limiter.
 *
 * O algoritmo ativo é definido via application.yaml — trocar a estratégia
 * não exige recompilação, apenas mudança de configuração.
 */
@Configuration
public class RateLimiterConfig {

    @Value("${rate-limiter.algorithm:token-bucket}")
    private String algorithm;

    @Value("${rate-limiter.capacity:10}")
    private long capacity;

    @Value("${rate-limiter.refill-rate-per-second:5}")
    private long refillRatePerSecond;

    @Value("${rate-limiter.window-size-millis:60000}")
    private long windowSizeMillis;

    @Bean
    public RateLimiter rateLimiter() {
        return switch (algorithm.toLowerCase()) {
            case "sliding-window" -> new SlidingWindowRateLimiter((int) capacity, windowSizeMillis);
            default              -> new TokenBucketRateLimiter(capacity, refillRatePerSecond);
        };
    }

    @Bean
    public RateLimitService rateLimitService(RateLimiter rateLimiter) {
        return new RateLimitService(rateLimiter);
    }
}