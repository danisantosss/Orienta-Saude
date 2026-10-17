package com.orientasaude.service;

/**
 * Resultado de uma rodada da entrevista adaptativa.
 * Ou traz a próxima pergunta, ou indica que a entrevista terminou
 * (inconclusiva quando atingiu o máximo de perguntas sem o Gemini concluir).
 */
public record ResultadoEntrevista(String pergunta, boolean completa, boolean inconclusiva) {

    public static ResultadoEntrevista novaPergunta(String pergunta) {
        return new ResultadoEntrevista(pergunta, false, false);
    }

    public static ResultadoEntrevista concluida() {
        return new ResultadoEntrevista(null, true, false);
    }

    public static ResultadoEntrevista inconclusivaPorLimite() {
        return new ResultadoEntrevista(null, true, true);
    }
}
