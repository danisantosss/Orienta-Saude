package com.orientasaude.ia;

import com.orientasaude.config.GeminiConfig;
import com.orientasaude.exception.IaIndisponivelException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o GeminiClient contra um servidor HTTP local que simula a API Gemini,
 * sem depender da chave real nem de acesso à internet.
 */
class GeminiClientTest {

    private HttpServer servidor;
    private String ultimoCaminho;
    private String ultimaChave;
    private String ultimoCorpo;
    private int statusResposta = 200;
    private String corpoResposta = "{}";
    private long atrasoMs = 0;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/", this::responder);
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private void responder(HttpExchange troca) throws IOException {
        ultimoCaminho = troca.getRequestURI().getPath();
        ultimaChave = troca.getRequestHeaders().getFirst("x-goog-api-key");
        ultimoCorpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (atrasoMs > 0) {
            try {
                Thread.sleep(atrasoMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = corpoResposta.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "application/json");
        troca.sendResponseHeaders(statusResposta, bytes.length);
        troca.getResponseBody().write(bytes);
        troca.close();
    }

    private GeminiClient criarCliente(String chave, long timeoutMs) {
        GeminiConfig config = new GeminiConfig();
        config.setApiKey(chave);
        config.setUrlBase("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta");
        config.setModeloTexto("modelo-texto");
        config.setModeloEmbedding("modelo-embedding");
        config.setTimeoutMs(timeoutMs);
        return new GeminiClient(config);
    }

    @Test
    void gerarTextoEnviaPromptERetornaTextoDaResposta() {
        corpoResposta = """
                {"candidates":[{"content":{"parts":[{"text":"Há quanto tempo"},{"text":" você tem febre?"}]}}]}
                """;

        String texto = criarCliente("chave-teste", 2000).gerarTexto("Paciente: 30 anos");

        assertThat(texto).isEqualTo("Há quanto tempo você tem febre?");
        assertThat(ultimoCaminho).isEqualTo("/v1beta/models/modelo-texto:generateContent");
        assertThat(ultimaChave).isEqualTo("chave-teste");
        assertThat(ultimoCorpo).contains("\"contents\"").contains("Paciente: 30 anos");
    }

    @Test
    void gerarEmbeddingRetornaVetorDeValores() {
        corpoResposta = """
                {"embedding":{"values":[0.5,-0.25,1.0]}}
                """;

        float[] vetor = criarCliente("chave-teste", 2000).gerarEmbedding("febre alta");

        assertThat(vetor).containsExactly(0.5f, -0.25f, 1.0f);
        assertThat(ultimoCaminho).isEqualTo("/v1beta/models/modelo-embedding:embedContent");
        assertThat(ultimoCorpo).contains("febre alta");
    }

    @Test
    void erroHttpDaApiViraIaIndisponivel() {
        statusResposta = 500;
        corpoResposta = "{\"error\":{\"message\":\"falha\"}}";

        assertThatThrownBy(() -> criarCliente("chave-teste", 2000).gerarTexto("prompt"))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void respostaSemTextoViraIaIndisponivel() {
        corpoResposta = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

        assertThatThrownBy(() -> criarCliente("chave-teste", 2000).gerarTexto("prompt"))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void respostaSemEmbeddingViraIaIndisponivel() {
        corpoResposta = "{}";

        assertThatThrownBy(() -> criarCliente("chave-teste", 2000).gerarEmbedding("texto"))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void chamadaQueExcedeOTimeoutViraIaIndisponivel() {
        atrasoMs = 2000;
        corpoResposta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"tarde demais\"}]}}]}";

        assertThatThrownBy(() -> criarCliente("chave-teste", 300).gerarTexto("prompt"))
                .isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void semChaveConfiguradaNaoChamaAApi() {
        assertThatThrownBy(() -> criarCliente("", 2000).gerarTexto("prompt"))
                .isInstanceOf(IaIndisponivelException.class);
        assertThat(ultimoCaminho).isNull();
    }
}
