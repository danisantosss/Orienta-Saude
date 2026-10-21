package com.orientasaude.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Busca por similaridade de cosseno com limiar, limite e alternativa por palavras-chave.
 */
class BuscadorDocumentosTest {

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final IndiceEmbeddings indice = mock(IndiceEmbeddings.class);
    private final RagConfig config = new RagConfig();
    private final BaseConhecimento base;
    private final BuscadorDocumentos buscador;

    BuscadorDocumentosTest() {
        config.setPadraoDocumentos("classpath:knowledge-base/*.json");
        config.setTimeoutBuscaMs(500);
        base = new BaseConhecimento(config, new ObjectMapper());
        buscador = new BuscadorDocumentos(base, indice, geminiClient, config);
    }

    /** Documento "febre-001" fica idêntico à consulta, "tosse-001" a 0,8 e o resto ortogonal. */
    private void comIndiceSimulado() {
        when(indice.garantirIndexado()).thenReturn(true);
        when(geminiClient.gerarEmbedding(anyString())).thenReturn(new float[]{1, 0});
        for (DocumentoConhecimento documento : base.listar()) {
            float[] vetor = switch (documento.id()) {
                case "febre-001" -> new float[]{1, 0};
                case "tosse-001" -> new float[]{0.8f, 0.6f};
                case "gripe-001" -> new float[]{0.6f, 0.8f};
                default -> new float[]{0, 1};
            };
            when(indice.vetorDe(documento.id())).thenReturn(Optional.of(vetor));
        }
    }

    @Test
    void retornaSoDocumentosAcimaDoLimiarOrdenadosPorSimilaridade() {
        comIndiceSimulado();

        ResultadoBusca resultado = buscador.buscar("febre alta e tosse");

        assertThat(resultado.metodo()).isEqualTo(MetodoBusca.EMBEDDINGS);
        assertThat(resultado.documentos()).extracting(encontrado -> encontrado.documento().id())
                .containsExactly("febre-001", "tosse-001");
        assertThat(resultado.documentos().get(0).relevancia()).isGreaterThan(resultado.documentos().get(1).relevancia());
    }

    @Test
    void respeitaOMaximoDeDocumentos() {
        comIndiceSimulado();
        config.setMaximoDocumentos(1);

        assertThat(buscador.buscar("febre").documentos()).hasSize(1);
    }

    @Test
    void semIndicePronto_usaPalavrasChave() {
        when(indice.garantirIndexado()).thenReturn(false);

        ResultadoBusca resultado = buscador.buscar("Estou com FEBRE e calafrio");

        assertThat(resultado.metodo()).isEqualTo(MetodoBusca.PALAVRAS_CHAVE);
        assertThat(resultado.documentos()).extracting(encontrado -> encontrado.documento().id()).contains("febre-001");
    }

    @Test
    void falhaAoGerarEmbeddingDaConsulta_usaPalavrasChave() {
        when(indice.garantirIndexado()).thenReturn(true);
        when(geminiClient.gerarEmbedding(anyString())).thenThrow(new IaIndisponivelException("fora"));

        ResultadoBusca resultado = buscador.buscar("dor de cabeça forte");

        assertThat(resultado.metodo()).isEqualTo(MetodoBusca.PALAVRAS_CHAVE);
        assertThat(resultado.documentos()).extracting(encontrado -> encontrado.documento().id()).contains("dor-de-cabeca-001");
    }

    @Test
    void buscaQueExcedeOTempoLimite_usaPalavrasChave() {
        when(indice.garantirIndexado()).thenReturn(true);
        when(geminiClient.gerarEmbedding(anyString())).thenAnswer(invocacao -> {
            Thread.sleep(2000);
            return new float[]{1, 0};
        });

        long inicio = System.currentTimeMillis();
        ResultadoBusca resultado = buscador.buscar("diarreia");

        assertThat(System.currentTimeMillis() - inicio).isLessThan(1500);
        assertThat(resultado.metodo()).isEqualTo(MetodoBusca.PALAVRAS_CHAVE);
    }

    @Test
    void palavrasChaveIgnoramAcentosEMaiusculas() {
        when(indice.garantirIndexado()).thenReturn(false);

        ResultadoBusca resultado = buscador.buscar("NAUSEA e VOMITO desde ontem");

        assertThat(resultado.documentos()).extracting(encontrado -> encontrado.documento().id()).contains("nausea-vomito-001");
    }

    @Test
    void consultaSemRelacaoNaoRetornaDocumentos() {
        when(indice.garantirIndexado()).thenReturn(false);

        ResultadoBusca resultado = buscador.buscar("xyz qwerty");

        assertThat(resultado.documentos()).isEmpty();
    }
}
