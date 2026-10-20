package com.orientasaude.rag;

import com.orientasaude.config.RagConfig;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda em memória o embedding de cada documento da base de conhecimento.
 * Os vetores são gerados uma vez, numa única chamada em lote ao Gemini. Se a chamada falhar,
 * a busca usa palavras-chave e a indexação é tentada de novo na próxima triagem.
 */
@Component
public class IndiceEmbeddings {

    private static final Logger log = LoggerFactory.getLogger(IndiceEmbeddings.class);

    private final BaseConhecimento base;
    private final GeminiClient geminiClient;
    private final RagConfig config;
    private volatile Map<String, float[]> vetores = Map.of();

    public IndiceEmbeddings(BaseConhecimento base, GeminiClient geminiClient, RagConfig config) {
        this.base = base;
        this.geminiClient = geminiClient;
        this.config = config;
    }

    /**
     * Gera os embeddings quando a aplicação termina de subir, sem travar a inicialização se o Gemini falhar.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void indexarNaInicializacao() {
        if (config.isIndexarNaInicializacao()) {
            garantirIndexado();
        }
    }

    /**
     * Indexa a base se ainda não estiver indexada. Devolve true quando os vetores estão prontos.
     */
    public synchronized boolean garantirIndexado() {
        if (!vetores.isEmpty()) {
            return true;
        }
        List<DocumentoConhecimento> documentos = base.listar();
        try {
            List<float[]> gerados = geminiClient.gerarEmbeddings(
                    documentos.stream().map(DocumentoConhecimento::textoIndexavel).toList());
            if (gerados.size() != documentos.size()) {
                log.warn("Indexação da base ignorada: {} vetores para {} documentos", gerados.size(), documentos.size());
                return false;
            }
            Map<String, float[]> novos = new HashMap<>();
            for (int i = 0; i < documentos.size(); i++) {
                novos.put(documentos.get(i).id(), gerados.get(i));
            }
            vetores = Map.copyOf(novos);
            log.info("Embeddings gerados para {} documentos da base de conhecimento", novos.size());
            return true;
        } catch (IaIndisponivelException e) {
            log.warn("Não foi possível gerar os embeddings da base; a busca vai usar palavras-chave");
            return false;
        }
    }

    public Optional<float[]> vetorDe(String idDocumento) {
        return Optional.ofNullable(vetores.get(idDocumento));
    }
}
