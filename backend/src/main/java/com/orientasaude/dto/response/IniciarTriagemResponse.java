package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IniciarTriagemResponse {
    private Long sessaoId;
    private String primeiraPergunta;
}