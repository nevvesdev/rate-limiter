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

@DisplayName("SlidingWindowRateLimiter")
class SlidingWindowRateLimiterTest {

    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // 5 requisições por janela de 1 segundo
        rateLimiter = new SlidingWindowRateLimiter(5, 1000);
    }

    @Test
    @DisplayName("deve permitir requisições dentro do limite da janela")
    void devePermitirRequisicoesDentroDoLimite() {
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimiter.tryAcquire("cliente-1");
            assertThat(result.isAllowed()).isTrue();
        }
    }

    @Test
    @DisplayName("deve negar requisição quando limite da janela for atingido")
    void deveNegarQuandoLimiteAtingido() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("cliente-1");
        }

        RateLimitResult result = rateLimiter.tryAcquire("cliente-1");
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfterMillis()).isPositive();
    }

    @Test
    @DisplayName("deve informar tempo correto de retry após bloqueio")
    void deveInformarRetryAfterCorreto() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("cliente-1");
        }

        RateLimitResult result = rateLimiter.tryAcquire("cliente-1");

        assertThat(result.isAllowed()).isFalse();
        // retryAfter deve ser <= windowSize (1000ms)
        assertThat(result.getRetryAfterMillis()).isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("deve liberar requisições após a janela deslizar")
    void deveLiberarAposJanelaDeslizar() throws InterruptedException {
        // Janela pequena para o teste não demorar
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 200);

        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("cliente-1");
        }

        // Bloqueado agora
        assertThat(limiter.tryAcquire("cliente-1").isAllowed()).isFalse();

        // Aguarda a janela deslizar
        Thread.sleep(250);

        // Deve liberar novamente
        assertThat(limiter.tryAcquire("cliente-1").isAllowed()).isTrue();
    }

    @Test
    @DisplayName("clientes diferentes devem ter janelas independentes")
    void clientesDiferentesDevemTerJanelasSeparadas() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("cliente-A");
        }

        RateLimitResult result = rateLimiter.tryAcquire("cliente-B");
        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("tokens restantes devem diminuir a cada requisição")
    void deveDecrementarTokensRestantes() {
        RateLimitResult primeira = rateLimiter.tryAcquire("cliente-1");
        RateLimitResult segunda  = rateLimiter.tryAcquire("cliente-1");

        assertThat(primeira.getRemainingTokens()).isGreaterThan(segunda.getRemainingTokens());
    }

    @Test
    @DisplayName("deve lançar exceção para limite de requisições inválido")
    void deveLancarExcecaoParaLimiteInvalido() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(0, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requisições");
    }

    @Test
    @DisplayName("deve lançar exceção para janela de tempo inválida")
    void deveLancarExcecaoParaJanelaInvalida() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("janela");
    }

    @Test
    @DisplayName("deve retornar nome correto do algoritmo")
    void deveRetornarNomeAlgoritmo() {
        assertThat(rateLimiter.algorithmName()).isEqualTo("SLIDING_WINDOW");
    }

    @Test
    @DisplayName("deve ser thread-safe sob alta concorrência")
    void deveSerThreadSafe() throws InterruptedException {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(10, 5000);
        int totalThreads = 50;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<RateLimitResult>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            futures.add(executor.submit(() -> {
                inicio.await();
                return limiter.tryAcquire("cliente-concorrente");
            }));
        }

        inicio.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long permitidas = futures.stream()
                .map(f -> {
                    try { return f.get(); }
                    catch (Exception e) { throw new RuntimeException(e); }
                })
                .filter(RateLimitResult::isAllowed)
                .count();

        // Exatamente 10 devem passar
        assertThat(permitidas).isEqualTo(10);
    }
}