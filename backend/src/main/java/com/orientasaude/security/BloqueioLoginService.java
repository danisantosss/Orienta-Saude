package com.orientasaude.security;

import com.orientasaude.exception.LoginBloqueadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia o login de um IP depois de várias senhas erradas seguidas (5 por padrão),
 * por um tempo (15 minutos por padrão). Dificulta adivinhar senhas por tentativa e erro.
 *
 * As contagens ficam em memória: são dados temporários e a aplicação roda numa réplica só.
 * Um login certo zera a contagem. Falhas antigas, sem nenhuma nova durante o tempo de bloqueio,
 * também deixam de contar, para o mapa não crescer para sempre.
 */
@Service
public class BloqueioLoginService {

    private final int maximoFalhas;
    private final Duration duracaoBloqueio;
    private final Clock relogio;
    private final Map<String, Tentativas> tentativasPorIp = new ConcurrentHashMap<>();

    public BloqueioLoginService(@Value("${app.seguranca.maximo-falhas-login:5}") int maximoFalhas,
                                @Value("${app.seguranca.minutos-bloqueio-login:15}") long minutosBloqueio,
                                Clock relogio) {
        this.maximoFalhas = maximoFalhas;
        this.duracaoBloqueio = Duration.ofMinutes(minutosBloqueio);
        this.relogio = relogio;
    }

    /** Lança LoginBloqueadoException se o IP estiver bloqueado agora. */
    public void verificar(String ip) {
        Tentativas tentativas = tentativasPorIp.get(ip);
        if (tentativas == null) {
            return;
        }
        Instant agora = relogio.instant();
        if (tentativas.bloqueadoAte() != null && agora.isBefore(tentativas.bloqueadoAte())) {
            long milissegundos = Duration.between(agora, tentativas.bloqueadoAte()).toMillis();
            throw new LoginBloqueadoException(Math.max((milissegundos + 999) / 1000, 1));
        }
        if (expirou(tentativas, agora)) {
            tentativasPorIp.remove(ip, tentativas);
        }
    }

    /**
     * Conta mais uma falha do IP.
     * @return true quando esta falha foi a que disparou o bloqueio
     */
    public boolean registrarFalha(String ip) {
        Instant agora = relogio.instant();
        Tentativas atualizadas = tentativasPorIp.compute(ip, (chave, anteriores) -> {
            int falhas = anteriores == null || expirou(anteriores, agora) ? 1 : anteriores.falhas() + 1;
            Instant bloqueadoAte = falhas >= maximoFalhas ? agora.plus(duracaoBloqueio) : null;
            return new Tentativas(falhas, agora, bloqueadoAte);
        });
        return atualizadas.falhas() == maximoFalhas;
    }

    /** Login certo: o IP volta a ter todas as tentativas. */
    public void registrarSucesso(String ip) {
        tentativasPorIp.remove(ip);
    }

    /** Limpa, a cada 10 minutos, os IPs cujas falhas ou bloqueio já venceram. */
    @Scheduled(fixedDelay = 600_000)
    public void limparExpirados() {
        Instant agora = relogio.instant();
        tentativasPorIp.entrySet().removeIf(entrada -> expirou(entrada.getValue(), agora));
    }

    int quantidadeDeIpsMonitorados() {
        return tentativasPorIp.size();
    }

    /** Bloqueio vencido, ou falhas sem bloqueio e sem nenhuma nova há mais que o tempo de bloqueio. */
    private boolean expirou(Tentativas tentativas, Instant agora) {
        if (tentativas.bloqueadoAte() != null) {
            return !agora.isBefore(tentativas.bloqueadoAte());
        }
        return !agora.isBefore(tentativas.ultimaFalha().plus(duracaoBloqueio));
    }

    private record Tentativas(int falhas, Instant ultimaFalha, Instant bloqueadoAte) {
    }
}
