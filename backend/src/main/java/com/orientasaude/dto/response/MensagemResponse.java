package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO genérico para respostas com apenas uma mensagem de texto.
 * Usado em confirmações de cadastro, exclusão, etc.
 */
@Data
@AllArgsConstructor
public class MensagemResponse {

    private String mensagem;
}
