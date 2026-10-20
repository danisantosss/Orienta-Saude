package com.orientasaude.ia;

import com.orientasaude.model.enums.NivelUrgencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 13.2: Propriedade 2 (urgência sempre válida) e Propriedade 4 (sem diagnóstico definitivo nem prescrição).
 */
class ValidadorEticoTest {

    private final ValidadorEtico validador = new ValidadorEtico();

    private AnaliseGerada analiseCom(String explicacao) {
        return new AnaliseGerada(NivelUrgencia.MODERADO, List.of("gripe"), explicacao,
                "Repouso e hidratação.", List.of("Falta de ar"), "Procure uma unidade de saúde se piorar.");
    }

    @Test
    void textoEducativoPassaSemViolacoes() {
        AnaliseGerada analise = analiseCom("Os sintomas podem estar relacionados a uma infecção viral.");

        assertThat(validador.validar(analise)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Você tem gripe.",
            "VOCE TEM uma infecção.",
            "Diagnóstico confirmado: sinusite.",
            "Você sofre de enxaqueca.",
            "Pelo quadro, foi diagnosticado com dengue.",
            "Paciente diagnosticada com infecção urinária."
    })
    void detectaCertezaDiagnostica(String texto) {
        assertThat(validador.validar(analiseCom(texto))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Tome paracetamol para a febre.",
            "Use Dipirona se tiver dor.",
            "Tome 500 mg ao dia.",
            "Um comprimido por dia.",
            "Tome de 8 em 8 horas.",
            "Pingue 20 gotas.",
            "Ibuprofeno ajuda na dor.",
            "Uma cápsula pela manhã."
    })
    void detectaPrescricao(String texto) {
        assertThat(validador.validar(analiseCom(texto))).isNotEmpty();
    }

    @Test
    void verificaTodosOsCamposDeTexto() {
        AnaliseGerada analise = new AnaliseGerada(NivelUrgencia.LEVE, List.of("Você tem gripe"),
                "Texto educativo.", "Tome amoxicilina.", List.of("Febre alta"), "Procure atendimento.");

        assertThat(validador.validar(analise)).hasSize(2);
    }

    @Test
    void naoConfundePalavrasParecidas() {
        AnaliseGerada analise = analiseCom(
                "Você temia que fosse grave, mas os sinais podem estar ligados a algo leve. Mantenha o descanso por 8 horas de sono.");

        assertThat(validador.validar(analise)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEVE", "moderado", " Urgente ", "EMERGÊNCIA", "emergencia"})
    void interpretaNiveisValidos(String valor) {
        assertThat(validador.interpretarNivel(valor)).isNotNull();
        assertThat(validador.interpretarNivel("leve")).isEqualTo(NivelUrgencia.LEVE);
        assertThat(validador.interpretarNivel("EMERGÊNCIA")).isEqualTo(NivelUrgencia.EMERGENCIA);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GRAVE", "nível 3", "baixo"})
    void nivelInvalidoOuAusenteViraUrgente(String valor) {
        assertThat(validador.interpretarNivel(valor)).isEqualTo(NivelUrgencia.URGENTE);
    }

    @Test
    void disclaimerOrientaProcurarAtendimentoESamu() {
        assertThat(ValidadorEtico.DISCLAIMER)
                .contains("não constituem diagnóstico")
                .contains("profissional de saúde")
                .contains("192");
    }
}
