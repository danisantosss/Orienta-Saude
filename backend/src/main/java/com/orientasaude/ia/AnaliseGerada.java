package com.orientasaude.ia;

import com.orientasaude.model.enums.NivelUrgencia;

import java.util.List;

/**
 * Análise final já interpretada a partir do JSON devolvido pelo Gemini.
 */
public record AnaliseGerada(
        NivelUrgencia nivelUrgencia,
        List<String> condicoes,
        String explicacaoEducativa,
        String orientacoesGerais,
        List<String> sinaisAlerta,
        String quandoBuscarAtendimento) {
}
