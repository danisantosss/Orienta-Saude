package com.orientasaude.rag;

/**
 * Similaridade de cosseno entre dois vetores de embedding: 1 = mesma direção (textos com o mesmo
 * sentido), 0 = sem relação, -1 = opostos. Não depende do tamanho dos vetores, só do ângulo.
 */
public final class SimilaridadeCosseno {

    private SimilaridadeCosseno() {
    }

    public static double calcular(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vetores com dimensões diferentes: " + a.length + " e " + b.length);
        }
        double produtoEscalar = 0;
        double normaA = 0;
        double normaB = 0;
        for (int i = 0; i < a.length; i++) {
            produtoEscalar += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        if (normaA == 0 || normaB == 0) {
            return 0;
        }
        return produtoEscalar / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }
}
