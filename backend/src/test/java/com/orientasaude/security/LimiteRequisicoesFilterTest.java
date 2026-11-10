package com.orientasaude.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orientasaude.RelogioAjustavel;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.service.AuditoriaService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LimiteRequisicoesFilterTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-09T10:00:15Z"));
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final RespostaErroJson respostaErroJson =
            new RespostaErroJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    private final LimiteRequisicoesFilter filtro =
            new LimiteRequisicoesFilter(3, relogio, auditoriaService, respostaErroJson);
    private final FilterChain chain = mock(FilterChain.class);

    private MockHttpServletResponse requisitar(String ip, String caminho) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", caminho);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, chain);
        return response;
    }

    @Test
    void deixaPassarAteOLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(requisitar("1.1.1.1", "/api/triagem/historico").getStatus()).isEqualTo(200);
        }
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    void passouDoLimiteRecebe429ComORetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            requisitar("1.1.1.1", "/api/triagem/historico");
        }

        MockHttpServletResponse resposta = requisitar("1.1.1.1", "/api/triagem/historico");

        assertThat(resposta.getStatus()).isEqualTo(429);
        // 10:00:15 -> faltam 45 s para virar o minuto
        assertThat(resposta.getHeader("Retry-After")).isEqualTo("45");
        assertThat(resposta.getContentAsString()).contains("\"status\":429").contains("Aguarde um minuto");
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    void registraNaAuditoriaSoAPrimeiraRecusaDoMinuto() throws Exception {
        for (int i = 0; i < 6; i++) {
            requisitar("1.1.1.1", "/api/triagem/historico");
        }

        verify(auditoriaService, times(1)).registrar(TipoEventoAuditoria.LIMITE_EXCEDIDO, null, "1.1.1.1");
    }

    @Test
    void cadaIpTemSuaPropriaContagem() throws Exception {
        for (int i = 0; i < 4; i++) {
            requisitar("1.1.1.1", "/api/triagem/historico");
        }

        assertThat(requisitar("2.2.2.2", "/api/triagem/historico").getStatus()).isEqualTo(200);
    }

    @Test
    void contagemZeraNaViradaDoMinuto() throws Exception {
        for (int i = 0; i < 4; i++) {
            requisitar("1.1.1.1", "/api/triagem/historico");
        }

        relogio.avancar(Duration.ofSeconds(45));

        assertThat(requisitar("1.1.1.1", "/api/triagem/historico").getStatus()).isEqualTo(200);
    }

    @Test
    void healthCheckNaoEntraNaConta() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(requisitar("1.1.1.1", "/actuator/health").getStatus()).isEqualTo(200);
        }
        assertThat(requisitar("1.1.1.1", "/api/auth/login").getStatus()).isEqualTo(200);
    }
}
