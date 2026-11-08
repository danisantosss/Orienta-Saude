package com.orientasaude.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pedido de exclusão da conta. A senha é pedida de novo para que um token roubado
 * ou um computador esquecido logado não bastem para apagar os dados de alguém.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcluirContaRequest {

    @NotBlank(message = "Informe sua senha para confirmar a exclusão")
    private String senha;
}
