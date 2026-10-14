package com.orientasaude.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a new user.
 * Collects only the minimum data required by LGPD: name, email and password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    private String name;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve estar em formato válido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres")
    private String password;
}
