package com.orientasaude.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requisição para cadastro de novo usuário.
 * Coleta apenas dados mínimos conforme LGPD: nome, email e senha.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    private String nome;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve estar em formato válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres")
    private String senha;
}
