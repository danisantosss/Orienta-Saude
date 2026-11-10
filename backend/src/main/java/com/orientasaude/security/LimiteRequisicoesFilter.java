package com.orientasaude.security;

import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.service.AuditoriaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita cada IP a um número de requisições por minuto na API (100 por padrão).
 * Passou do limite, recebe HTTP 429 até virar o minuto (janela fixa: 10:00:00 a 10:00:59, depois zera).
 *
 * Roda antes do Spring Security (ordem alta), então barra o excesso antes de qualquer consulta
 * ao banco. Só vale para /api/**: o /actuator/health, usado pelo Azure para saber se o contêiner
 * está vivo, não entra na conta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LimiteRequisicoesFilter extends OncePerRequestFilter {

    private static final long MILISSEGUNDOS_POR_MINUTO = 60_000;

    private final int limitePorMinuto;
    private final Clock relogio;
    private final AuditoriaService auditoriaService;
    private final RespostaErroJson respostaErroJson;

    private final Map<String, AtomicInteger> contagemPorIp = new ConcurrentHashMap<>();
    private volatile long minutoAtual = -1;

    public LimiteRequisicoesFilter(@Value("${app.seguranca.limite-requisicoes-por-minuto:100}") int limitePorMinuto,
                                   Clock relogio,
                                   AuditoriaService auditoriaService,
                                   RespostaErroJson respostaErroJson) {
        this.limitePorMinuto = limitePorMinuto;
        this.relogio = relogio;
        this.auditoriaService = auditoriaService;
        this.respostaErroJson = respostaErroJson;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long agora = relogio.millis();
        iniciarNovaJanelaSeVirouOMinuto(agora / MILISSEGUNDOS_POR_MINUTO);

        String ip = request.getRemoteAddr();
        int requisicoes = contagemPorIp.computeIfAbsent(ip, chave -> new AtomicInteger()).incrementAndGet();

        if (requisicoes > limitePorMinuto) {
            // Registra só a primeira recusa da janela, para um ataque não encher a tabela de auditoria
            if (requisicoes == limitePorMinuto + 1) {
                auditoriaService.registrar(TipoEventoAuditoria.LIMITE_EXCEDIDO, null, ip);
            }
            long segundosAteVirar = (MILISSEGUNDOS_POR_MINUTO - agora % MILISSEGUNDOS_POR_MINUTO + 999) / 1000;
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundosAteVirar));
            respostaErroJson.escrever(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas Requisições",
                    "Você fez muitas requisições em pouco tempo. Aguarde um minuto e tente novamente.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Na virada do minuto as contagens recomeçam do zero (e o mapa não cresce sem fim). */
    private void iniciarNovaJanelaSeVirouOMinuto(long minuto) {
        if (minuto != minutoAtual) {
            synchronized (this) {
                if (minuto != minutoAtual) {
                    contagemPorIp.clear();
                    minutoAtual = minuto;
                }
            }
        }
    }
}
