package com.orientasaude.model.enums;

/**
 * Papel de quem enviou a mensagem na entrevista adaptativa.
 * SISTEMA: mensagem inicial com sintomas
 * USUARIO: resposta do paciente
 * ASSISTENTE: pergunta gerada pela IA
 */
public enum PapelMensagem {
    SISTEMA,
    USUARIO,
    ASSISTENTE
}
