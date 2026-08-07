package br.com.nevvesdev.ratelimiter.core.algorithm;

import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenBucketRateLimiter")
class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // capacidade 5, reposição de 1 token/segundo
        rateLimiter = new TokenBucketRateLimiter(5, 1);
    }

    @Test
    @DisplayName("deve permitir requisições dentro da capacidade do balde")
    void devePermitirRequisicoesDentroCapacidade() {
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimiter.tryAcquire("cliente-1");
            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Test
    @DisplayName("deve negar requisição quando balde estiver vazio")
    void deveNegarQuandoBaldeVazio() {
        // Esgota todos os tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("cliente-1");
        }

        RateLimitResult result = rateLimiter.tryAcquire("cliente-1");
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfterMillis()).isPositive();
    }

    @Test
    @DisplayName("deve retornar tokens restantes decrescendo a cada requisição")
    void deveDecrementarTokensRestantes() {
        RateLimitResult primeira = rateLimiter.tryAcquire("cliente-1");
        RateLimitResult segunda = rateLimiter.tryAcquire("cliente-1");

        assertThat(primeira.getRemainingTokens()).isGreaterThan(segunda.getRemainingTokens());
    }

    @Test
    @DisplayName("clientes diferentes devem ter baldes independentes")
    void clientesDiferentesDevemTerBaldeSeparados() {
        // Esgota tokens do cliente-A
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("cliente-A");
        }

        // cliente-B não deve ser afetado
        RateLimitResult result = rateLimiter.tryAcquire("cliente-B");
        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("deve lançar exceção para capacidade inválida")
    void deveLancarExcecaoParaCapacidadeInvalida() {
        assertThatThrownBy(() -> new TokenBucketRateLimiter(0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacidade");
    }

    @Test
    @DisplayName("deve lançar exceção para taxa de reposição inválida")
    void deveLancarExcecaoParaTaxaReposicaoInvalida() {
        assertThatThrownBy(() -> new TokenBucketRateLimiter(5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reposição");
    }

    @Test
    @DisplayName("deve retornar nome correto do algoritmo")
    void deveRetornarNomeAlgoritmo() {
        assertThat(rateLimiter.algorithmName()).isEqualTo("TOKEN_BUCKET");
    }

    @Test
    @DisplayName("deve ser thread-safe sob alta concorrência")
    void deveSerThreadSafe() throws InterruptedException {
        // 10 capacidade, 50 threads tentando ao mesmo tempo
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 1);
        int totalThreads = 50;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<RateLimitResult>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> {
                inicio.await(); // todas disparam juntas
                return limiter.tryAcquire("cliente-concorrente");
            }));
        }

        inicio.countDown(); // dispara todas as threads simultaneamente
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long permitidas = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { throw new RuntimeException(e); }
                })
                .filter(RateLimitResult::isAllowed)
                .count();

        long negadas = totalThreads - permitidas;

        // Exatamente 10 devem passar, 40 devem ser negadas
        assertThat(permitidas).isEqualTo(10);
        assertThat(negadas).isEqualTo(40);
    }
}