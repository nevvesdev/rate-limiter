package br.com.nevvesdev.ratelimiter.infrastructure.web.controller;

import br.com.nevvesdev.ratelimiter.application.RateLimitService;
import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;
import br.com.nevvesdev.ratelimiter.infrastructure.web.dto.RateLimitRequest;
import br.com.nevvesdev.ratelimiter.infrastructure.web.dto.RateLimitResponse;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limit")
@Tag(name = "Rate Limiter", description = "Verificação e status do rate limiting")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/check")
    @Operation(summary = "Verifica se o cliente está dentro do limite de requisições")
    public ResponseEntity<RateLimitResponse> check(@Valid @RequestBody RateLimitRequest request) {
        RateLimitResult result = rateLimitService.checkLimit(request.clientId());
        RateLimitResponse response = RateLimitResponse.from(result, rateLimitService.activeAlgorithm());

        if (result.isAllowed()) {
            return ResponseEntity.ok(response);
        }

        // 429 Too Many Requests — padrão HTTP para rate limit excedido
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Retorna o algoritmo ativo e configurações gerais")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "algoritmo", rateLimitService.activeAlgorithm(),
                "status", "ativo"
        ));
    }
}