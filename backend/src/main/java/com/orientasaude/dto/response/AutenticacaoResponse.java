package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de resposta após login bem-sucedido.
 * Contém o token JWT e o tempo de expiração.
 */
@Data
@AllArgsConstructor
public class AutenticacaoResponse {

    private String token;
    private long expiraEm;
}
