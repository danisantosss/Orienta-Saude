package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Dados que o usuário vê sobre si mesmo. Não expõe id nem senha.
 */
@Data
@Builder
@AllArgsConstructor
public class PerfilResponse {
    private String nome;
    private String email;
    private LocalDateTime criadoEm;
    private LocalDateTime consentimentoEm;
    private String versaoPolitica;
}
