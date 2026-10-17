package com.orientasaude.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.orientasaude.config.GeminiConfig;
import com.orientasaude.exception.IaIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP da API Gemini (REST).
 * Toda falha (erro HTTP, timeout, resposta vazia ou chave ausente) vira IaIndisponivelException,
 * para que as camadas de cima tratem um único tipo de erro.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiConfig config;
    private final RestClient restClient;

    public GeminiClient(GeminiConfig config) {
        this.config = config;

        Duration timeout = Duration.ofMillis(config.getTimeoutMs());
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(config.getUrlBase())
                .requestFactory(fabrica)
                .defaultHeader("x-goog-api-key", config.getApiKey() == null ? "" : config.getApiKey())
                .build();
    }

    /**
     * Envia o prompt ao modelo de texto e devolve o texto gerado.
     */
    public String gerarTexto(String prompt) {
        Map<String, Object> corpo = Map.of("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))));

        JsonNode resposta = chamar("/models/" + config.getModeloTexto() + ":generateContent", corpo);

        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : resposta.path("candidates").path(0).path("content").path("parts")) {
            texto.append(parte.path("text").asText(""));
        }
        if (texto.isEmpty()) {
            throw new IaIndisponivelException("A API Gemini não devolveu texto");
        }
        return texto.toString();
    }

    /**
     * Gera o vetor de embedding do texto, usado na busca semântica do RAG.
     */
    public float[] gerarEmbedding(String texto) {
        Map<String, Object> corpo = Map.of("content", Map.of(
                "parts", List.of(Map.of("text", texto))));

        JsonNode valores = chamar("/models/" + config.getModeloEmbedding() + ":embedContent", corpo)
                .path("embedding").path("values");
        if (!valores.isArray() || valores.isEmpty()) {
            throw new IaIndisponivelException("A API Gemini não devolveu o embedding");
        }

        float[] vetor = new float[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            vetor[i] = (float) valores.get(i).asDouble();
        }
        return vetor;
    }

    private JsonNode chamar(String caminho, Map<String, Object> corpo) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IaIndisponivelException("Chave da API Gemini não configurada");
        }
        try {
            JsonNode resposta = restClient.post()
                    .uri(caminho)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);
            if (resposta == null) {
                throw new IaIndisponivelException("Resposta vazia da API Gemini");
            }
            return resposta;
        } catch (RestClientException e) {
            // Não registra o prompt: ele contém dados de saúde do usuário
            log.warn("Falha ao chamar a API Gemini em {}: {}", caminho, e.getClass().getSimpleName());
            throw new IaIndisponivelException("Falha ao chamar a API Gemini", e);
        }
    }
}
