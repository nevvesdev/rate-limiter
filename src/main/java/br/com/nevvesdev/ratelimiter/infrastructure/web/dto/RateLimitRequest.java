package br.com.nevvesdev.ratelimiter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RateLimitRequest(

        @NotBlank(message = "clientId é obrigatório")
        String clientId
) {}