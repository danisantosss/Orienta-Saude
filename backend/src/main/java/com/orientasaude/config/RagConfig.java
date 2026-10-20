package com.orientasaude.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Parâmetros do RAG lidos do application.yml (prefixo app.rag).
 */
@Configuration
@ConfigurationProperties(prefix = "app.rag")
@Data
public class RagConfig {

    /** Similaridade de cosseno mínima para um documento entrar no contexto (Requisito 5.2). */
    private double limiarSimilaridade = 0.7;

    /** Quantidade máxima de documentos enviados ao prompt. */
    private int maximoDocumentos = 10;

    /** Tempo máximo da busca por embeddings; depois disso usa palavras-chave (orçamento de 8 s). */
    private long timeoutBuscaMs = 8000;

    /** Gera os embeddings da base quando a aplicação sobe (desligado nos testes). */
    private boolean indexarNaInicializacao = true;

    /** Onde ficam os documentos JSON da base de conhecimento. */
    private String padraoDocumentos = "classpath:knowledge-base/*.json";
}
