package com.orientasaude.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnviarMensagemRequest {

    @NotBlank(message = "O conteúdo da mensagem é obrigatório")
    @Size(max = 1000, message = "A resposta deve ter no máximo 1000 caracteres")
    private String conteudo;
}