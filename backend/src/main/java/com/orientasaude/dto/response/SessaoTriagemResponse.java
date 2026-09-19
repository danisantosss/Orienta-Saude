package com.orientasaude.dto.response;

import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.StatusSessao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class SessaoTriagemResponse {
    private Long id;
    private String sintomasIniciais;
    private Integer idadePaciente;
    private StatusSessao status;
    private LocalDateTime iniciadaEm;
    private LocalDateTime finalizadaEm;
    private NivelUrgencia nivelUrgencia;
}