package com.orientasaude.service;

import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import com.orientasaude.ia.PromptBuilder;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.repository.MensagemTriagemRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa as regras da entrevista adaptativa com o Gemini simulado (mock).
 * Propriedade 5: mínimo de 2 e máximo de 10 perguntas complementares.
 */
class EntrevistaServiceTest {

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final MensagemTriagemRepository mensagemRepository = mock(MensagemTriagemRepository.class);
    private final EntrevistaService service =
            new EntrevistaService(geminiClient, new PromptBuilder(), mensagemRepository);

    private final SessaoTriagem sessao = SessaoTriagem.builder()
            .id(1L)
            .idadePaciente(30)
            .sintomasIniciais("Febre e dor no corpo")
            .build();

    private void comPerguntasJaFeitas(int quantidade) {
        List<MensagemTriagem> mensagens = new ArrayList<>();
        int ordem = 1;
        mensagens.add(MensagemTriagem.builder().papel(PapelMensagem.SISTEMA)
                .conteudo("Febre e dor no corpo").ordemSequencia(ordem++).build());
        for (int i = 1; i <= quantidade; i++) {
            mensagens.add(MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE)
                    .conteudo("Pergunta " + i + "?").ordemSequencia(ordem++).build());
            mensagens.add(MensagemTriagem.builder().papel(PapelMensagem.USUARIO)
                    .conteudo("Resposta " + i).ordemSequencia(ordem++).build());
        }
        when(mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(1L)).thenReturn(mensagens);
    }

    @Test
    void primeiraPerguntaEhGeradaPeloGemini() {
        comPerguntasJaFeitas(0);
        when(geminiClient.gerarTexto(anyString())).thenReturn("  Há quanto tempo você tem febre?  ");

        ResultadoEntrevista resultado = service.gerarProximaPergunta(sessao);

        assertThat(resultado.completa()).isFalse();
        assertThat(resultado.pergunta()).isEqualTo("Há quanto tempo você tem febre?");
    }

    @Test
    void marcadorDeConclusaoAposMinimoEncerraAEntrevista() {
        comPerguntasJaFeitas(2);
        when(geminiClient.gerarTexto(anyString())).thenReturn("[ENTREVISTA_COMPLETA]");

        ResultadoEntrevista resultado = service.gerarProximaPergunta(sessao);

        assertThat(resultado.completa()).isTrue();
        assertThat(resultado.inconclusiva()).isFalse();
        assertThat(resultado.pergunta()).isNull();
    }

    @Test
    void marcadorComTextoAoRedorTambemEncerra() {
        comPerguntasJaFeitas(3);
        when(geminiClient.gerarTexto(anyString())).thenReturn("Obrigado. [ENTREVISTA_COMPLETA]");

        assertThat(service.gerarProximaPergunta(sessao).completa()).isTrue();
    }

    @Test
    void marcadorAntesDoMinimoPedeNovamenteUmaPergunta() {
        comPerguntasJaFeitas(1);
        when(geminiClient.gerarTexto(anyString()))
                .thenReturn("[ENTREVISTA_COMPLETA]")
                .thenReturn("A dor piora à noite?");

        ResultadoEntrevista resultado = service.gerarProximaPergunta(sessao);

        assertThat(resultado.completa()).isFalse();
        assertThat(resultado.pergunta()).isEqualTo("A dor piora à noite?");
        verify(geminiClient, times(2)).gerarTexto(anyString());
    }

    @Test
    void marcadorRepetidoAntesDoMinimoViraIaIndisponivel() {
        comPerguntasJaFeitas(0);
        when(geminiClient.gerarTexto(anyString())).thenReturn("[ENTREVISTA_COMPLETA]");

        assertThatThrownBy(() -> service.gerarProximaPergunta(sessao))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void apos10PerguntasEncerraComoInconclusivaSemChamarOGemini() {
        comPerguntasJaFeitas(10);

        ResultadoEntrevista resultado = service.gerarProximaPergunta(sessao);

        assertThat(resultado.completa()).isTrue();
        assertThat(resultado.inconclusiva()).isTrue();
        verify(geminiClient, never()).gerarTexto(anyString());
    }

    @Test
    void respostaVaziaDoGeminiViraIaIndisponivel() {
        comPerguntasJaFeitas(2);
        when(geminiClient.gerarTexto(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> service.gerarProximaPergunta(sessao))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void falhaDoGeminiEhPropagadaParaPermitirRepetir() {
        comPerguntasJaFeitas(2);
        when(geminiClient.gerarTexto(anyString())).thenThrow(new IaIndisponivelException("fora do ar"));

        assertThatThrownBy(() -> service.gerarProximaPergunta(sessao))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void contaApenasPerguntasDoAssistente() {
        comPerguntasJaFeitas(4);

        assertThat(service.contarPerguntasFeitas(sessao)).isEqualTo(4);
    }
}
