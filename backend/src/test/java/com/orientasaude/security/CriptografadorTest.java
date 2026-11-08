package com.orientasaude.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriptografadorTest {

    private static final String CHAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OUTRA_CHAVE = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final Criptografador criptografador = new Criptografador(CHAVE);

    @Test
    void cifraEDecifraDevolvendoOTextoOriginal() {
        String texto = "Dor no peito e falta de ar desde ontem à noite";

        String cifrado = criptografador.cifrar(texto);

        assertThat(cifrado).startsWith("enc:v1:").doesNotContain("peito");
        assertThat(criptografador.decifrar(cifrado)).isEqualTo(texto);
    }

    @Test
    void mesmoTextoGeraCifrasDiferentesPorCausaDoIvAleatorio() {
        assertThat(criptografador.cifrar("febre")).isNotEqualTo(criptografador.cifrar("febre"));
    }

    @Test
    void textoGravadoAntesDaCriptografiaVoltaSemAlteracao() {
        assertThat(criptografador.decifrar("Sintomas antigos em texto puro")).isEqualTo("Sintomas antigos em texto puro");
    }

    @Test
    void nuloContinuaNulo() {
        assertThat(criptografador.cifrar(null)).isNull();
        assertThat(criptografador.decifrar(null)).isNull();
    }

    @Test
    void cifraAdulteradaEhRecusada() {
        String cifrado = criptografador.cifrar("tosse seca");
        byte[] bytes = Base64.getDecoder().decode(cifrado.substring("enc:v1:".length()));
        bytes[bytes.length - 1] ^= 1;
        String adulterado = "enc:v1:" + Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> criptografador.decifrar(adulterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void outraChaveNaoConsegueDecifrar() {
        String cifrado = criptografador.cifrar("dor de cabeça");

        assertThatThrownBy(() -> new Criptografador(OUTRA_CHAVE).decifrar(cifrado))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaChaveQueNaoTem256Bits() {
        String chaveCurta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new Criptografador(chaveCurta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new Criptografador(""))
                .isInstanceOf(IllegalStateException.class);
    }
}
