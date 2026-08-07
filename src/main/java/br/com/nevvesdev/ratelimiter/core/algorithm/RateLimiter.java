package br.com.nevvesdev.ratelimiter.core.algorithm;

import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;

/**
 Contrato central do rate limiter.
 Cada algoritmo implementa essa interface — o Spring nunca toca aqui.
 */
public interface RateLimiter {

    /**
     Verifica se a requisição do cliente identificado por {@code clientId}
     está dentro do limite permitido.

     @param clientId identificador único do cliente (ex: IP, userId, apiKey)
     @return resultado indicando se a requisição foi permitida ou bloqueada
     */
    RateLimitResult tryAcquire(String clientId);

    /**
     Retorna o nome do algoritmo — útil para logging e para a API informar
     qual estratégia está ativa.
     */
    String algorithmName();
}