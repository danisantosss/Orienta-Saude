package com.orientasaude.service;

import com.orientasaude.dto.response.IndicadoresResponse;
import com.orientasaude.dto.response.IndicadoresResponse.TotalPorPeriodo;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.repository.ResultadoTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula os indicadores do painel administrativo direto no banco, com COUNT e GROUP BY.
 * Nenhuma triagem individual é carregada: o serviço nunca vê sintomas nem dados de quem fez.
 * O nível de urgência e as datas ficam em claro no banco justamente para permitir essas contas
 * sem decifrar nada.
 */
@Service
@RequiredArgsConstructor
public class IndicadoresService {

    static final int PERIODO_PADRAO_DIAS = 30;
    static final int PERIODO_MAXIMO_DIAS = 366;
    static final int LIMITE_AGRUPAMENTO_DIARIO = 62;

    private final SessaoTriagemRepository sessaoRepository;
    private final ResultadoTriagemRepository resultadoRepository;
    private final Clock relogio;

    /**
     * Indicadores das triagens iniciadas entre inicio e fim (os dois dias inclusos).
     * Sem datas, usa os últimos 30 dias até hoje.
     */
    @Transactional(readOnly = true)
    public IndicadoresResponse calcular(LocalDate inicio, LocalDate fim) {
        LocalDate ultimoDia = fim != null ? fim : LocalDate.now(relogio);
        LocalDate primeiroDia = inicio != null ? inicio : ultimoDia.minusDays(PERIODO_PADRAO_DIAS - 1L);
        validarPeriodo(primeiroDia, ultimoDia);

        LocalDateTime de = primeiroDia.atStartOfDay();
        LocalDateTime ate = ultimoDia.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> porDia = new HashMap<>();
        for (Object[] linha : sessaoRepository.contarPorDia(de, ate)) {
            porDia.put((LocalDate) linha[0], (Long) linha[1]);
        }
        long total = porDia.values().stream().mapToLong(Long::longValue).sum();

        boolean porMes = ChronoUnit.DAYS.between(primeiroDia, ultimoDia) + 1 > LIMITE_AGRUPAMENTO_DIARIO;
        List<TotalPorPeriodo> serie = porMes
                ? agruparPorMes(porDia, primeiroDia, ultimoDia)
                : agruparPorDia(porDia, primeiroDia, ultimoDia);

        Map<NivelUrgencia, Long> distribuicao = new EnumMap<>(NivelUrgencia.class);
        for (NivelUrgencia nivel : NivelUrgencia.values()) {
            distribuicao.put(nivel, 0L);
        }
        for (Object[] linha : resultadoRepository.contarPorNivelUrgencia(de, ate)) {
            distribuicao.put((NivelUrgencia) linha[0], (Long) linha[1]);
        }

        long finalizadas = 0;
        long antecipadas = 0;
        for (Object[] linha : sessaoRepository.contarPorMotivoEncerramento(de, ate)) {
            long quantidade = (Long) linha[1];
            finalizadas += quantidade;
            if (linha[0] == MotivoEncerramento.ANTECIPADO) {
                antecipadas = quantidade;
            }
        }

        return new IndicadoresResponse(primeiroDia, ultimoDia, porMes ? "MES" : "DIA", total, serie,
                distribuicao, finalizadas, antecipadas, porcentagem(antecipadas, finalizadas));
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new EntradaInvalidaException("A data final não pode ser anterior à data inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > PERIODO_MAXIMO_DIAS) {
            throw new EntradaInvalidaException("O período pode ter no máximo 366 dias");
        }
    }

    private static List<TotalPorPeriodo> agruparPorDia(Map<LocalDate, Long> porDia, LocalDate inicio, LocalDate fim) {
        List<TotalPorPeriodo> serie = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            serie.add(new TotalPorPeriodo(dia, porDia.getOrDefault(dia, 0L)));
        }
        return serie;
    }

    private static List<TotalPorPeriodo> agruparPorMes(Map<LocalDate, Long> porDia, LocalDate inicio, LocalDate fim) {
        Map<LocalDate, Long> porMes = new LinkedHashMap<>();
        for (LocalDate mes = inicio.withDayOfMonth(1); !mes.isAfter(fim); mes = mes.plusMonths(1)) {
            porMes.put(mes, 0L);
        }
        porDia.forEach((dia, total) -> porMes.merge(dia.withDayOfMonth(1), total, Long::sum));
        return porMes.entrySet().stream()
                .map(mes -> new TotalPorPeriodo(mes.getKey(), mes.getValue()))
                .toList();
    }

    private static double porcentagem(long parte, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(parte * 1000.0 / total) / 10.0;
    }
}
