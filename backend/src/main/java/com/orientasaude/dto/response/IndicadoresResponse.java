package com.orientasaude.dto.response;

import com.orientasaude.model.enums.NivelUrgencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Indicadores do painel administrativo. Só números agregados: nenhum nome, email,
 * id de usuário ou texto de sintomas sai por aqui.
 *
 * @param agrupamento                 DIA para períodos de até 62 dias; MES para períodos maiores
 * @param triagensPorPeriodo          total de triagens em cada dia (ou mês) do período, inclusive os zerados
 * @param distribuicaoUrgencia        resultados por nível de urgência, sempre com os quatro níveis
 * @param triagensFinalizadas         entrevistas que terminaram (por qualquer motivo)
 * @param encerradasAntecipadamente   entrevistas que o usuário encerrou antes da conclusão
 * @param taxaEncerramentoAntecipado  encerradas antecipadamente / finalizadas, em %, com uma casa decimal
 */
public record IndicadoresResponse(
        LocalDate inicio,
        LocalDate fim,
        String agrupamento,
        long totalTriagens,
        List<TotalPorPeriodo> triagensPorPeriodo,
        Map<NivelUrgencia, Long> distribuicaoUrgencia,
        long triagensFinalizadas,
        long encerradasAntecipadamente,
        double taxaEncerramentoAntecipado) {

    /** Total de triagens iniciadas a partir de "inicio" (o dia, ou o primeiro dia do mês). */
    public record TotalPorPeriodo(LocalDate inicio, long total) {
    }
}
