package com.orientasaude.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IniciarTriagemRequest {

    @NotBlank(message = "Os sintomas são obrigatórios")
    @Size(min = 10, max = 1000, message = "Os sintomas devem ter entre 10 e 1000 caracteres")
    private String sintomas;

    @NotNull(message = "A idade é obrigatória")
    @Min(value = 1, message = "A idade deve ser no mínimo 1")
    @Max(value = 120, message = "A idade deve ser no máximo 120")
    private Integer idade;
}