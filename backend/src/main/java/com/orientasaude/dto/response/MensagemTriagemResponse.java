package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MensagemTriagemResponse {
    private String pergunta;
    private boolean completa;
}