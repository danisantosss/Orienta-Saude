package com.orientasaude.service;

import com.orientasaude.RelogioAjustavel;
import com.orientasaude.dto.response.IndicadoresResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.repository.ResultadoTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicadoresServiceTest {

    private final SessaoTriagemRepository sessaoRepository = mock(SessaoTriagemRepository.class);
    private final ResultadoTriagemRepository resultadoRepository = mock(ResultadoTriagemRepository.class);
    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-11-09T12:00:00Z"));
    private final IndicadoresService service = new IndicadoresService(sessaoRepository, resultadoRepository, relogio);

    private static Object[] linha(Object chave, long total) {
        return new Object[] {chave, total};
    }

    @Test
    void semDatasUsaOsUltimosTrintaDias() {
        IndicadoresResponse resposta = service.calcular(null, null);

        assertThat(resposta.inicio()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(resposta.fim()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(resposta.triagensPorPeriodo()).hasSize(30);
        verify(sessaoRepository).contarPorDia(LocalDateTime.of(2026, 10, 11, 0, 0), LocalDateTime.of(2026, 11, 10, 0, 0));
    }

    @Test
    void montaASerieDiariaComOsDiasSemTriagemZerados() {
        when(sessaoRepository.contarPorDia(any(), any())).thenReturn(List.of(
                linha(LocalDate.of(2026, 11, 1), 3L),
                linha(LocalDate.of(2026, 11, 3), 2L)));

        IndicadoresResponse resposta = service.calcular(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

        assertThat(resposta.agrupamento()).isEqualTo("DIA");
        assertThat(resposta.totalTriagens()).isEqualTo(5);
        assertThat(resposta.triagensPorPeriodo()).extracting(IndicadoresResponse.TotalPorPeriodo::total)
                .containsExactly(3L, 0L, 2L);
    }

    @Test
    void periodoLongoAgrupaPorMes() {
        when(sessaoRepository.contarPorDia(any(), any())).thenReturn(List.of(
                linha(LocalDate.of(2026, 9, 10), 4L),
                linha(LocalDate.of(2026, 9, 20), 1L),
                linha(LocalDate.of(2026, 11, 2), 2L)));

        IndicadoresResponse resposta = service.calcular(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 11, 9));

        assertThat(resposta.agrupamento()).isEqualTo("MES");
        assertThat(resposta.triagensPorPeriodo()).extracting(IndicadoresResponse.TotalPorPeriodo::inicio)
                .containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
        assertThat(resposta.triagensPorPeriodo()).extracting(IndicadoresResponse.TotalPorPeriodo::total)
                .containsExactly(0L, 5L, 0L, 2L);
    }

    @Test
    void distribuicaoSempreTrazOsQuatroNiveis() {
        when(resultadoRepository.contarPorNivelUrgencia(any(), any())).thenReturn(List.<Object[]>of(
                linha(NivelUrgencia.LEVE, 6L),
                linha(NivelUrgencia.EMERGENCIA, 1L)));

        IndicadoresResponse resposta = service.calcular(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9));

        assertThat(resposta.distribuicaoUrgencia()).containsExactly(
                java.util.Map.entry(NivelUrgencia.LEVE, 6L),
                java.util.Map.entry(NivelUrgencia.MODERADO, 0L),
                java.util.Map.entry(NivelUrgencia.URGENTE, 0L),
                java.util.Map.entry(NivelUrgencia.EMERGENCIA, 1L));
    }

    @Test
    void taxaDeEncerramentoAntecipadoEmPorcentagem() {
        when(sessaoRepository.contarPorMotivoEncerramento(any(), any())).thenReturn(List.<Object[]>of(
                linha(MotivoEncerramento.SUFICIENTE, 4L),
                linha(MotivoEncerramento.LIMITE_PERGUNTAS, 1L),
                linha(MotivoEncerramento.ANTECIPADO, 1L)));

        IndicadoresResponse resposta = service.calcular(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9));

        assertThat(resposta.triagensFinalizadas()).isEqualTo(6);
        assertThat(resposta.encerradasAntecipadamente()).isEqualTo(1);
        assertThat(resposta.taxaEncerramentoAntecipado()).isEqualTo(16.7);
    }

    @Test
    void semTriagensFinalizadasATaxaEhZero() {
        IndicadoresResponse resposta = service.calcular(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 9));

        assertThat(resposta.totalTriagens()).isZero();
        assertThat(resposta.taxaEncerramentoAntecipado()).isZero();
    }

    @Test
    void dataFinalAntesDaInicialEhRecusada() {
        assertThatThrownBy(() -> service.calcular(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(EntradaInvalidaException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void periodoMaiorQueUmAnoEhRecusado() {
        assertThatThrownBy(() -> service.calcular(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 9)))
                .isInstanceOf(EntradaInvalidaException.class)
                .hasMessageContaining("366 dias");
    }
}
