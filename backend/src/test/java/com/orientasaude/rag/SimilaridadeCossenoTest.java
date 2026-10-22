package com.orientasaude.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SimilaridadeCossenoTest {

    @Test
    void vetoresIguaisTemSimilaridade1() {
        assertThat(SimilaridadeCosseno.calcular(new float[]{1, 2, 3}, new float[]{1, 2, 3})).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void vetoresOrtogonaisTemSimilaridade0() {
        assertThat(SimilaridadeCosseno.calcular(new float[]{1, 0}, new float[]{0, 1})).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void vetoresOpostosTemSimilaridadeMenos1() {
        assertThat(SimilaridadeCosseno.calcular(new float[]{1, 1}, new float[]{-1, -1})).isCloseTo(-1.0, within(1e-6));
    }

    @Test
    void naoDependeDoTamanhoDoVetor() {
        assertThat(SimilaridadeCosseno.calcular(new float[]{1, 2}, new float[]{10, 20})).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void vetorZeradoTemSimilaridade0() {
        assertThat(SimilaridadeCosseno.calcular(new float[]{0, 0}, new float[]{1, 2})).isZero();
    }

    @Test
    void dimensoesDiferentesSaoRecusadas() {
        assertThatThrownBy(() -> SimilaridadeCosseno.calcular(new float[]{1}, new float[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
