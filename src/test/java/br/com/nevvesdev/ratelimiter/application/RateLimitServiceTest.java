package br.com.nevvesdev.ratelimiter.application;

import br.com.nevvesdev.ratelimiter.core.algorithm.RateLimiter;
import br.com.nevvesdev.ratelimiter.core.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
class RateLimitServiceTest {

    @Mock
    private RateLimiter rateLimiter;

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitService(rateLimiter);
    }

    @Test
    @DisplayName("deve delegar tryAcquire ao algoritmo configurado")
    void deveDelegarAoAlgoritmo() {
        RateLimitResult expected = RateLimitResult.allowed(4);
        when(rateLimiter.tryAcquire("cliente-1")).thenReturn(expected);

        RateLimitResult result = service.checkLimit("cliente-1");

        assertThat(result).isEqualTo(expected);
        verify(rateLimiter, times(1)).tryAcquire("cliente-1");
    }

    @Test
    @DisplayName("deve propagar resultado de negação do algoritmo")
    void devePropagarlResultadoNegado() {
        RateLimitResult expected = RateLimitResult.denied(500);
        when(rateLimiter.tryAcquire("cliente-1")).thenReturn(expected);

        RateLimitResult result = service.checkLimit("cliente-1");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRetryAfterMillis()).isEqualTo(500);
    }

    @Test
    @DisplayName("deve lançar exceção para clientId nulo")
    void deveLancarExcecaoParaClienteNulo() {
        assertThatThrownBy(() -> service.checkLimit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId");

        verifyNoInteractions(rateLimiter);
    }

    @Test
    @DisplayName("deve lançar exceção para clientId vazio")
    void deveLancarExcecaoParaClienteVazio() {
        assertThatThrownBy(() -> service.checkLimit("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId");

        verifyNoInteractions(rateLimiter);
    }

    @Test
    @DisplayName("deve fazer trim no clientId antes de delegar")
    void deveFazerTrimNoClientId() {
        RateLimitResult expected = RateLimitResult.allowed(3);
        when(rateLimiter.tryAcquire("cliente-1")).thenReturn(expected);

        service.checkLimit("  cliente-1  ");

        verify(rateLimiter).tryAcquire("cliente-1");
    }

    @Test
    @DisplayName("deve retornar nome do algoritmo ativo")
    void deveRetornarNomeDoAlgoritmo() {
        when(rateLimiter.algorithmName()).thenReturn("TOKEN_BUCKET");

        assertThat(service.activeAlgorithm()).isEqualTo("TOKEN_BUCKET");
    }

    @Test
    @DisplayName("deve lançar exceção ao construir com rateLimiter nulo")
    void deveLancarExcecaoComRateLimiterNulo() {
        assertThatThrownBy(() -> new RateLimitService(null))
                .isInstanceOf(NullPointerException.class);
    }
}