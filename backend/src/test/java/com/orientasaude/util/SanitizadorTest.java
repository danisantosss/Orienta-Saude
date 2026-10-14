package com.orientasaude.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizadorTest {

    @Test
    void retornaNuloQuandoTextoNulo() {
        assertThat(Sanitizador.sanitizar(null)).isNull();
    }

    @Test
    void removeTagsHtml() {
        assertThat(Sanitizador.sanitizar("<script>alert(1)</script>dor de cabeça"))
                .isEqualTo("alert(1)dor de cabeça");
    }

    @Test
    void removeSequenciasSql() {
        assertThat(Sanitizador.sanitizar("febre; DROP TABLE usuarios -- /* x */"))
                .isEqualTo("febre DROP TABLE usuarios x");
    }

    @Test
    void colapsaEspacosExtrasERemoveBordas() {
        assertThat(Sanitizador.sanitizar("   tosse    seca   ")).isEqualTo("tosse seca");
    }
}
