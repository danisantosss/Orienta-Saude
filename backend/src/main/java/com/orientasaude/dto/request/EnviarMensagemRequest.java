package com.orientasaude.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnviarMensagemRequest {

    @NotBlank(message = "O conteúdo da mensagem é obrigatório")
    private String conteudo;
}