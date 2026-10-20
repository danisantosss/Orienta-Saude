package com.orientasaude.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.PapelMensagem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task 12.5: consulta, contexto para o prompt e avisos de limitação (Requisitos 5.1, 5.2, 5.4, 5.8).
 */
class RagServiceTest {

    private final BuscadorDocumentos buscador = mock(BuscadorDocumentos.class);
    private final RagService service = new RagService(buscador);
    private final List<DocumentoConhecimento> documentos;

    private final SessaoTriagem sessao = SessaoTriagem.builder()
            .id(1L).idadePaciente(40).sintomasIniciais("Febre e dor no corpo").build();
    private final List<MensagemTriagem> mensagens = List.of(
            MensagemTriagem.builder().papel(PapelMensagem.SISTEMA).conteudo("Febre e dor no corpo").build(),
            MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE).conteudo("Há quanto tempo?").build(),
            MensagemTriagem.builder().papel(PapelMensagem.USUARIO).conteudo("Três dias").build());

    RagServiceTest() {
        RagConfig config = new RagConfig();
        config.setPadraoDocumentos("classpath:knowledge-base/*.json");
        documentos = new BaseConhecimento(config, new ObjectMapper()).listar();
    }

    private ResultadoBusca resultadoCom(int quantidade, MetodoBusca metodo) {
        return new ResultadoBusca(documentos.subList(0, quantidade).stream()
                .map(documento -> new DocumentoEncontrado(documento, 0.9)).toList(), metodo);
    }

    @Test
    void consultaUsaSintomasERespostasSemAsPerguntas() {
        when(buscador.buscar(anyString())).thenReturn(resultadoCom(3, MetodoBusca.EMBEDDINGS));

        service.recuperar(sessao, mensagens);

        ArgumentCaptor<String> consulta = ArgumentCaptor.forClass(String.class);
        verify(buscador).buscar(consulta.capture());
        assertThat(consulta.getValue()).contains("Febre e dor no corpo").contains("Três dias").doesNotContain("Há quanto tempo?");
    }

    @Test
    void tresOuMaisDocumentosNaoGeramAviso() {
        when(buscador.buscar(anyString())).thenReturn(resultadoCom(3, MetodoBusca.EMBEDDINGS));

        ContextoRag contexto = service.recuperar(sessao, mensagens);

        assertThat(contexto.notaLimitacao()).isNull();
        assertThat(contexto.idsDocumentos()).hasSize(3);
        assertThat(contexto.texto()).contains(documentos.get(0).fonte()).contains(documentos.get(0).urlFonte());
    }

    @Test
    void menosDeTresDocumentosGeramAvisoDeLimitacao() {
        when(buscador.buscar(anyString())).thenReturn(resultadoCom(2, MetodoBusca.PALAVRAS_CHAVE));

        ContextoRag contexto = service.recuperar(sessao, mensagens);

        assertThat(contexto.notaLimitacao()).isEqualTo(RagService.NOTA_POUCOS_DOCUMENTOS);
        assertThat(contexto.idsDocumentos()).hasSize(2);
    }

    @Test
    void nenhumDocumentoSegueSoComAIaEComAviso() {
        when(buscador.buscar(anyString())).thenReturn(new ResultadoBusca(List.of(), MetodoBusca.INDISPONIVEL));

        ContextoRag contexto = service.recuperar(sessao, mensagens);

        assertThat(contexto.notaLimitacao()).isEqualTo(RagService.NOTA_SEM_DOCUMENTOS);
        assertThat(contexto.idsDocumentos()).isEmpty();
        assertThat(contexto.texto()).isEqualTo(RagService.CONTEXTO_VAZIO);
    }
}
