package com.orientasaude.model.enums;

/**
 * Por que a entrevista adaptativa terminou.
 * SUFICIENTE: o Gemini indicou que tem informações suficientes
 * LIMITE_PERGUNTAS: atingiu 10 perguntas sem concluir (coleta inconclusiva)
 * ANTECIPADO: o usuário encerrou antes da conclusão
 */
public enum MotivoEncerramento {
    SUFICIENTE,
    LIMITE_PERGUNTAS,
    ANTECIPADO
}
