package com.orientasaude.model.enums;

/**
 * Papel de quem enviou a mensagem na entrevista adaptativa.
 * SISTEMA: mensagem inicial com sintomas
 * USUARIO: resposta do paciente
 * ASSISTENTE: pergunta da entrevista gerada pela IA (só perguntas; avisos não são gravados como mensagem)
 */
public enum PapelMensagem {
    SISTEMA,
    USUARIO,
    ASSISTENTE
}
