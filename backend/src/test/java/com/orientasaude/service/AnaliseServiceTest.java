package com.orientasaude.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.AnaliseGerada;
import com.orientasaude.ia.GeminiClient;
import com.orientasaude.ia.PromptBuilder;
import com.orientasaude.ia.ValidadorEtico;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.rag.ContextoRag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Análise final com contexto do RAG, parse do JSON, novas tentativas e limite de tempo.
 */
class AnaliseServiceTest {

    private static final String JSON_VALIDO = """
            {
              "nivelUrgencia": "MODERADO",
              "condicoes": ["gripe", "resfriado comum"],
              "explicacaoEducativa": "Os sintomas podem estar relacionados a uma infecção viral.",
              "orientacoesGerais": "Repouso e hidratação.",
              "sinaisAlerta": ["Falta de ar", "Febre acima de 39 °C por mais de 3 dias"],
              "quandoBuscarAtendimento": "Procure uma unidade básica de saúde se não melhorar em 3 dias."
            }
            """;

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final AnaliseService service = criarService(30000);

    private final SessaoTriagem sessao = SessaoTriagem.builder()
            .id(1L).idadePaciente(30).sintomasIniciais("Febre e dor no corpo").build();
    private final List<MensagemTriagem> mensagens = List.of(
            MensagemTriagem.builder().papel(PapelMensagem.SISTEMA).conteudo("Febre e dor no corpo").build(),
            MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE).conteudo("Há quanto tempo?").build(),
            MensagemTriagem.builder().papel(PapelMensagem.USUARIO).conteudo("Dois dias").build());
    private final ContextoRag contexto = new ContextoRag("[gripe-001] Fonte: Ministério da Saúde", List.of("gripe-001"), null);

    private AnaliseService criarService(long limiteTotalMs) {
        return new AnaliseService(geminiClient, new PromptBuilder(), new ValidadorEtico(), new ObjectMapper(), 0, limiteTotalMs);
    }

    private String jsonCom(String campo, String valor) {
        return JSON_VALIDO.replaceFirst("\"" + campo + "\": [^\\n]*", "\"" + campo + "\": " + valor + ",");
    }

    @Test
    void interpretaORespostaEmJsonDoGemini() {
        when(geminiClient.gerarJson(anyString())).thenReturn(JSON_VALIDO);

        AnaliseGerada analise = service.analisar(sessao, mensagens, contexto);

        assertThat(analise.nivelUrgencia()).isEqualTo(NivelUrgencia.MODERADO);
        assertThat(analise.condicoes()).containsExactly("gripe", "resfriado comum");
        assertThat(analise.explicacaoEducativa()).startsWith("Os sintomas podem");
        assertThat(analise.orientacoesGerais()).isEqualTo("Repouso e hidratação.");
        assertThat(analise.sinaisAlerta()).hasSize(2);
        assertThat(analise.quandoBuscarAtendimento()).contains("unidade básica");
    }

    @Test
    void promptLevaOContextoRecuperadoPeloRag() {
        when(geminiClient.gerarJson(anyString())).thenReturn(JSON_VALIDO);

        service.analisar(sessao, mensagens, contexto);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).gerarJson(prompt.capture());
        assertThat(prompt.getValue()).contains("[gripe-001] Fonte: Ministério da Saúde").contains("Resposta: Dois dias");
    }

    @Test
    void aceitaJsonDentroDeBlocoDeCodigo() {
        when(geminiClient.gerarJson(anyString())).thenReturn("```json\n" + JSON_VALIDO + "\n```");

        assertThat(service.analisar(sessao, mensagens, contexto).nivelUrgencia()).isEqualTo(NivelUrgencia.MODERADO);
    }

    @Test
    void nivelInvalidoViraUrgente() {
        when(geminiClient.gerarJson(anyString())).thenReturn(jsonCom("nivelUrgencia", "\"GRAVISSIMO\""));

        assertThat(service.analisar(sessao, mensagens, contexto).nivelUrgencia()).isEqualTo(NivelUrgencia.URGENTE);
    }

    @Test
    void maisDeCincoCondicoesSaoLimitadasACinco() {
        when(geminiClient.gerarJson(anyString()))
                .thenReturn(jsonCom("condicoes", "[\"a\", \"b\", \"c\", \"d\", \"e\", \"f\", \"g\"]"));

        assertThat(service.analisar(sessao, mensagens, contexto).condicoes()).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void jsonInvalidoNaPrimeiraTentativaTentaDeNovo() {
        when(geminiClient.gerarJson(anyString())).thenReturn("isto não é json").thenReturn(JSON_VALIDO);

        assertThat(service.analisar(sessao, mensagens, contexto).nivelUrgencia()).isEqualTo(NivelUrgencia.MODERADO);
        verify(geminiClient, times(2)).gerarJson(anyString());
    }

    @Test
    void semCondicoesOuCampoObrigatorioVazioContaComoRespostaInvalida() {
        when(geminiClient.gerarJson(anyString()))
                .thenReturn(jsonCom("condicoes", "[]"))
                .thenReturn(jsonCom("explicacaoEducativa", "\"\""))
                .thenReturn(JSON_VALIDO);

        assertThat(service.analisar(sessao, mensagens, contexto).condicoes()).isNotEmpty();
        verify(geminiClient, times(3)).gerarJson(anyString());
    }

    @Test
    void falhaDoGeminiEmTodasAsTentativasViraIaIndisponivel() {
        when(geminiClient.gerarJson(anyString())).thenThrow(new IaIndisponivelException("fora do ar"));

        assertThatThrownBy(() -> service.analisar(sessao, mensagens, contexto))
                .isInstanceOf(IaIndisponivelException.class);
        verify(geminiClient, times(3)).gerarJson(anyString());
    }

    @Test
    void violacaoEticaGeraPromptDeReforcoComOsTermosEncontrados() {
        when(geminiClient.gerarJson(anyString()))
                .thenReturn(jsonCom("orientacoesGerais", "\"Tome paracetamol de 8 em 8 horas.\""))
                .thenReturn(JSON_VALIDO);

        AnaliseGerada analise = service.analisar(sessao, mensagens, contexto);

        assertThat(analise.orientacoesGerais()).isEqualTo("Repouso e hidratação.");
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(geminiClient, times(2)).gerarJson(prompts.capture());
        assertThat(prompts.getAllValues().get(1)).contains("paracetamol").contains("Reescreva");
    }

    @Test
    void violacaoEticaPersistenteViraIaIndisponivel() {
        when(geminiClient.gerarJson(anyString())).thenReturn(jsonCom("explicacaoEducativa", "\"Você tem gripe.\""));

        assertThatThrownBy(() -> service.analisar(sessao, mensagens, contexto))
                .isInstanceOf(IaIndisponivelException.class);
        verify(geminiClient, times(3)).gerarJson(anyString());
    }

    @Test
    void respeitaOLimiteTotalDeTempo() {
        AnaliseService comLimiteCurto = criarService(300);
        when(geminiClient.gerarJson(anyString())).thenAnswer(invocacao -> {
            Thread.sleep(2000);
            return JSON_VALIDO;
        });

        long inicio = System.currentTimeMillis();
        assertThatThrownBy(() -> comLimiteCurto.analisar(sessao, mensagens, contexto))
                .isInstanceOf(IaIndisponivelException.class);
        assertThat(System.currentTimeMillis() - inicio).isLessThan(1500);
    }
}
