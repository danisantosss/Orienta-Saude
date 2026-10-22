package com.orientasaude.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Embeddings dos documentos gerados uma vez e mantidos em memória.
 */
class IndiceEmbeddingsTest {

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final RagConfig config = new RagConfig();
    private final BaseConhecimento base;
    private final IndiceEmbeddings indice;

    IndiceEmbeddingsTest() {
        config.setPadraoDocumentos("classpath:knowledge-base/*.json");
        base = new BaseConhecimento(config, new ObjectMapper());
        indice = new IndiceEmbeddings(base, geminiClient, config);
    }

    private List<float[]> vetores(int quantidade) {
        return Collections.nCopies(quantidade, new float[]{0.1f, 0.2f});
    }

    @Test
    void indexaTodosOsDocumentosEmUmaChamadaEGuardaEmMemoria() {
        int total = base.listar().size();
        when(geminiClient.gerarEmbeddings(anyList())).thenReturn(vetores(total));

        assertThat(indice.garantirIndexado()).isTrue();
        assertThat(indice.garantirIndexado()).isTrue();

        verify(geminiClient, times(1)).gerarEmbeddings(anyList());
        assertThat(indice.vetorDe(base.listar().get(0).id())).isPresent();
    }

    @Test
    void falhaNaIndexacaoNaoDerrubaETentaDeNovoDepois() {
        int total = base.listar().size();
        when(geminiClient.gerarEmbeddings(anyList()))
                .thenThrow(new IaIndisponivelException("fora do ar"))
                .thenReturn(vetores(total));

        assertThat(indice.garantirIndexado()).isFalse();
        assertThat(indice.garantirIndexado()).isTrue();
    }

    @Test
    void quantidadeErradaDeVetoresNaoContaComoIndexado() {
        when(geminiClient.gerarEmbeddings(anyList())).thenReturn(vetores(1));

        assertThat(indice.garantirIndexado()).isFalse();
        assertThat(indice.vetorDe(base.listar().get(0).id())).isEmpty();
    }

    @Test
    void indexacaoNaInicializacaoRespeitaAConfiguracao() {
        config.setIndexarNaInicializacao(false);

        indice.indexarNaInicializacao();

        verify(geminiClient, never()).gerarEmbeddings(anyList());
    }

    @Test
    void indexacaoNaInicializacaoAtivadaGeraOsVetores() {
        config.setIndexarNaInicializacao(true);
        when(geminiClient.gerarEmbeddings(anyList())).thenReturn(vetores(base.listar().size()));

        indice.indexarNaInicializacao();

        verify(geminiClient).gerarEmbeddings(anyList());
    }
}
