package com.orientasaude.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-256-bits-para-hs256";

    private final JwtTokenProvider provider = new JwtTokenProvider(SEGREDO, 60_000);

    @Test
    void gerarTokenPermiteExtrairUsuarioId() {
        String token = provider.gerarToken(42L, "maria@exemplo.com");

        assertThat(provider.validarToken(token)).isTrue();
        assertThat(provider.extrairUsuarioId(token)).isEqualTo(42L);
        assertThat(provider.getExpiracaoMs()).isEqualTo(60_000);
    }

    @Test
    void tokenMalformadoEhInvalido() {
        assertThat(provider.validarToken("nao-e-um-jwt")).isFalse();
        assertThat(provider.validarToken("")).isFalse();
    }

    @Test
    void tokenAssinadoComOutraChaveEhInvalido() {
        JwtTokenProvider outro = new JwtTokenProvider("outro-segredo-com-pelo-menos-256-bits-para-hs256-x", 60_000);

        assertThat(provider.validarToken(outro.gerarToken(1L, "a@exemplo.com"))).isFalse();
    }

    @Test
    void tokenExpiradoEhInvalido() {
        JwtTokenProvider expirado = new JwtTokenProvider(SEGREDO, -1_000);

        assertThat(provider.validarToken(expirado.gerarToken(1L, "a@exemplo.com"))).isFalse();
    }
}
