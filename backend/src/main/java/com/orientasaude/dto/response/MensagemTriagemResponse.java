package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Resposta a cada mensagem da entrevista: a próxima pergunta ou o aviso de que ela terminou.
 * O aviso só aparece quando a coleta foi inconclusiva ou encerrada antes da hora.
 */
@Data
@AllArgsConstructor
public class MensagemTriagemResponse {
    private String pergunta;
    private boolean completa;
    private String aviso;
}
