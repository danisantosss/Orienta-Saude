package com.orientasaude.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da API Gemini lida do application.yml (prefixo app.gemini).
 * A chave vem da variável de ambiente GEMINI_API_KEY e nunca fica no código.
 */
@Configuration
@ConfigurationProperties(prefix = "app.gemini")
@Data
public class GeminiConfig {

    private String apiKey;
    private String urlBase = "https://generativelanguage.googleapis.com/v1beta";
    private String modeloTexto = "gemini-3.5-flash-lite";
    private String modeloEmbedding = "gemini-embedding-001";
    private long timeoutMs = 10000;
}
