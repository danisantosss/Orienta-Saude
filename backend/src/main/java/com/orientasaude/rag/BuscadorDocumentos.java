package com.orientasaude.rag;

import com.orientasaude.config.RagConfig;
import com.orientasaude.ia.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Recupera os documentos mais parecidos com a consulta.
 * Caminho principal: embedding da consulta + similaridade de cosseno, filtro pelo limiar (0,7)
 * e no máximo 10 documentos. Se os embeddings falharem ou passarem do tempo limite (8 s),
 * usa busca por palavras-chave, para a triagem nunca parar por causa do RAG (Requisitos 5.6 e 5.7).
 */
@Component
public class BuscadorDocumentos {

    private static final Logger log = LoggerFactory.getLogger(BuscadorDocumentos.class);

    private final BaseConhecimento base;
    private final IndiceEmbeddings indice;
    private final GeminiClient geminiClient;
    private final RagConfig config;

    public BuscadorDocumentos(BaseConhecimento base, IndiceEmbeddings indice, GeminiClient geminiClient, RagConfig config) {
        this.base = base;
        this.indice = indice;
        this.geminiClient = geminiClient;
        this.config = config;
    }

    public ResultadoBusca buscar(String consulta) {
        try {
            Optional<List<DocumentoEncontrado>> porEmbeddings = buscarPorEmbeddingsComTempoLimite(consulta);
            if (porEmbeddings.isPresent()) {
                return new ResultadoBusca(porEmbeddings.get(), MetodoBusca.EMBEDDINGS);
            }
            return new ResultadoBusca(buscarPorPalavrasChave(consulta), MetodoBusca.PALAVRAS_CHAVE);
        } catch (RuntimeException e) {
            log.error("Falha inesperada na busca da base de conhecimento: {}", e.getClass().getSimpleName());
            return new ResultadoBusca(List.of(), MetodoBusca.INDISPONIVEL);
        }
    }

    /**
     * Vazio quando os embeddings não estão disponíveis (falha, índice não pronto ou tempo esgotado).
     */
    private Optional<List<DocumentoEncontrado>> buscarPorEmbeddingsComTempoLimite(String consulta) {
        CompletableFuture<Optional<List<DocumentoEncontrado>>> busca =
                CompletableFuture.supplyAsync(() -> buscarPorEmbeddings(consulta));
        try {
            return busca.get(config.getTimeoutBuscaMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            busca.cancel(true);
            log.warn("Busca por embeddings passou de {} ms; usando palavras-chave", config.getTimeoutBuscaMs());
        } catch (ExecutionException e) {
            log.warn("Busca por embeddings falhou ({}); usando palavras-chave", e.getCause().getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    private Optional<List<DocumentoEncontrado>> buscarPorEmbeddings(String consulta) {
        if (!indice.garantirIndexado()) {
            return Optional.empty();
        }
        float[] vetorConsulta = geminiClient.gerarEmbedding(consulta);

        List<DocumentoEncontrado> encontrados = new ArrayList<>();
        for (DocumentoConhecimento documento : base.listar()) {
            Optional<float[]> vetor = indice.vetorDe(documento.id());
            if (vetor.isEmpty()) {
                continue;
            }
            double similaridade = SimilaridadeCosseno.calcular(vetorConsulta, vetor.get());
            if (similaridade >= config.getLimiarSimilaridade()) {
                encontrados.add(new DocumentoEncontrado(documento, similaridade));
            }
        }
        return Optional.of(ordenarELimitar(encontrados));
    }

    /**
     * Alternativa sem IA: conta quantas palavras-chave do documento (e o próprio sintoma) aparecem
     * na consulta, sem diferenciar maiúsculas e acentos. Relevância = termos encontrados / termos do documento.
     */
    private List<DocumentoEncontrado> buscarPorPalavrasChave(String consulta) {
        String consultaNormalizada = normalizar(consulta);
        List<DocumentoEncontrado> encontrados = new ArrayList<>();
        for (DocumentoConhecimento documento : base.listar()) {
            List<String> termos = new ArrayList<>(documento.palavrasChave());
            termos.add(documento.sintoma());
            long acertos = termos.stream()
                    .map(BuscadorDocumentos::normalizar)
                    .filter(termo -> !termo.isBlank() && consultaNormalizada.contains(termo))
                    .count();
            if (acertos > 0) {
                encontrados.add(new DocumentoEncontrado(documento, (double) acertos / termos.size()));
            }
        }
        return ordenarELimitar(encontrados);
    }

    private List<DocumentoEncontrado> ordenarELimitar(List<DocumentoEncontrado> encontrados) {
        return encontrados.stream()
                .sorted(Comparator.comparingDouble(DocumentoEncontrado::relevancia).reversed())
                .limit(config.getMaximoDocumentos())
                .toList();
    }

    /**
     * Minúsculas, sem acentos e só letras e números separados por espaço, com espaços nas pontas
     * para comparar palavras inteiras ("dor" não casa com "dormir").
     */
    static String normalizar(String texto) {
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String simples = semAcentos.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        return simples.isEmpty() ? "" : " " + simples + " ";
    }
}
