package com.orientasaude.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requisição para login do usuário.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve estar em formato válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;
}
