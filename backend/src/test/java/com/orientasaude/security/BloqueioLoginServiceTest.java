package com.orientasaude.security;

import com.orientasaude.RelogioAjustavel;
import com.orientasaude.exception.LoginBloqueadoException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloqueioLoginServiceTest {

    private static final String IP = "200.1.2.3";

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-09T10:00:00Z"));
    private final BloqueioLoginService service = new BloqueioLoginService(5, 15, relogio);

    private void errar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            service.registrarFalha(IP);
        }
    }

    @Test
    void quatroFalhasAindaDeixamTentar() {
        errar(4);

        assertThatCode(() -> service.verificar(IP)).doesNotThrowAnyException();
    }

    @Test
    void aQuintaFalhaSeguidaBloqueiaPorQuinzeMinutos() {
        errar(4);

        assertThat(service.registrarFalha(IP)).isTrue();
        assertThatThrownBy(() -> service.verificar(IP))
                .isInstanceOf(LoginBloqueadoException.class)
                .hasMessageContaining("15 minutos")
                .extracting("segundosRestantes").isEqualTo(900L);
    }

    @Test
    void soAFalhaQueDisparaOBloqueioAvisa() {
        errar(4);

        assertThat(service.registrarFalha(IP)).isTrue();
        assertThat(service.registrarFalha(IP)).isFalse();
    }

    @Test
    void bloqueioVenceDepoisDeQuinzeMinutos() {
        errar(5);

        relogio.avancar(Duration.ofMinutes(14).plusSeconds(59));
        assertThatThrownBy(() -> service.verificar(IP))
                .isInstanceOf(LoginBloqueadoException.class)
                .hasMessageContaining("1 minuto");

        relogio.avancar(Duration.ofSeconds(1));
        assertThatCode(() -> service.verificar(IP)).doesNotThrowAnyException();
        // Depois do bloqueio a contagem recomeça: uma falha nova não bloqueia de novo
        assertThat(service.registrarFalha(IP)).isFalse();
        assertThatCode(() -> service.verificar(IP)).doesNotThrowAnyException();
    }

    @Test
    void loginCertoZeraAsFalhas() {
        errar(4);
        service.registrarSucesso(IP);
        errar(4);

        assertThatCode(() -> service.verificar(IP)).doesNotThrowAnyException();
    }

    @Test
    void bloqueioDeUmIpNaoAfetaOutro() {
        errar(5);

        assertThatCode(() -> service.verificar("200.9.9.9")).doesNotThrowAnyException();
    }

    @Test
    void falhasAntigasDeixamDeContar() {
        errar(4);
        relogio.avancar(Duration.ofMinutes(16));

        assertThat(service.registrarFalha(IP)).isFalse();
        assertThatCode(() -> service.verificar(IP)).doesNotThrowAnyException();
    }

    @Test
    void limpezaRemoveSoOsIpsVencidos() {
        errar(2);
        relogio.avancar(Duration.ofMinutes(20));
        service.registrarFalha("200.9.9.9");

        service.limparExpirados();

        assertThat(service.quantidadeDeIpsMonitorados()).isOne();
    }

    @Test
    void verificarRemoveRegistroVencido() {
        errar(2);
        relogio.avancar(Duration.ofMinutes(20));

        service.verificar(IP);

        assertThat(service.quantidadeDeIpsMonitorados()).isZero();
    }
}
