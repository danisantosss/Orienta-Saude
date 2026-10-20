package com.orientasaude.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import com.orientasaude.repository.MensagemTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.model.enums.StatusSessao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração da triagem pela API (tasks 9.4, 11.2 e 13.4), com MySQL real (Testcontainers)
 * e o Gemini simulado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TriagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private MensagemTriagemRepository mensagemRepository;

    @MockBean
    private GeminiClient geminiClient;

    private String cadastrarELogar() throws Exception {
        String email = "usuario-" + UUID.randomUUID() + "@exemplo.com";
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Pessoa Teste\", \"email\": \"" + email + "\", \"senha\": \"senhaSegura123\"}"))
                .andExpect(status().isCreated());
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"senhaSegura123\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(corpo).get("token").asText();
    }

    private long iniciarTriagem(String token) throws Exception {
        String corpo = mockMvc.perform(post("/api/triagem/iniciar")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sintomas\": \"Febre alta e dor no corpo desde ontem\", \"idade\": 30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primeiraPergunta").value("Há quanto tempo você tem febre?"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(corpo);
        return json.get("sessaoId").asLong();
    }

    private void responder(String token, long sessaoId, String conteudo) throws Exception {
        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"" + conteudo + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void entrevistaCompletaDePontaAPonta() throws Exception {
        String token = cadastrarELogar();
        when(geminiClient.gerarTexto(anyString()))
                .thenReturn("Há quanto tempo você tem febre?")
                .thenReturn("A febre passa com antitérmico?")
                .thenReturn("[ENTREVISTA_COMPLETA]");

        long sessaoId = iniciarTriagem(token);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Desde ontem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pergunta").value("A febre passa com antitérmico?"))
                .andExpect(jsonPath("$.completa").value(false));

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Passa por algumas horas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completa").value(true));

        assertThat(sessaoRepository.findById(sessaoId).orElseThrow().getStatus()).isEqualTo(StatusSessao.ANALISANDO);
        assertThat(mensagemRepository.countBySessaoId(sessaoId)).isEqualTo(5);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Mais uma\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void iniciarComDadosInvalidosRetorna400() throws Exception {
        String token = cadastrarELogar();

        mockMvc.perform(post("/api/triagem/iniciar")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sintomas\": \"curto\", \"idade\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.sintomas").exists())
                .andExpect(jsonPath("$.campos.idade").exists());
    }

    @Test
    void semTokenNaoAcessaATriagem() throws Exception {
        mockMvc.perform(post("/api/triagem/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sintomas\": \"Febre alta desde ontem\", \"idade\": 30}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessaoDeOutroUsuarioRetorna404() throws Exception {
        String tokenDono = cadastrarELogar();
        String tokenOutro = cadastrarELogar();
        when(geminiClient.gerarTexto(anyString())).thenReturn("Há quanto tempo você tem febre?");
        long sessaoId = iniciarTriagem(tokenDono);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Invasão\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/triagem/" + sessaoId + "/encerrar").header("Authorization", tokenOutro))
                .andExpect(status().isNotFound());

        assertThat(mensagemRepository.countBySessaoId(sessaoId)).isEqualTo(2);
    }

    @Test
    void falhaDoGeminiRetorna503ESemPerderOContextoPermiteRepetir() throws Exception {
        String token = cadastrarELogar();
        when(geminiClient.gerarTexto(anyString()))
                .thenReturn("Há quanto tempo você tem febre?")
                .thenThrow(new IaIndisponivelException("timeout"))
                .thenReturn("A febre passa com antitérmico?");
        long sessaoId = iniciarTriagem(token);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Desde ontem\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(mensagemRepository.countBySessaoId(sessaoId)).isEqualTo(2);

        responder(token, sessaoId, "Desde ontem");
        assertThat(mensagemRepository.countBySessaoId(sessaoId)).isEqualTo(4);
    }

    @Test
    void encerrarAntecipadamenteRetornaAviso() throws Exception {
        String token = cadastrarELogar();
        when(geminiClient.gerarTexto(anyString())).thenReturn("Há quanto tempo você tem febre?");
        long sessaoId = iniciarTriagem(token);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/encerrar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completa").value(true))
                .andExpect(jsonPath("$.aviso").isNotEmpty());

        assertThat(sessaoRepository.findById(sessaoId).orElseThrow().getStatus()).isEqualTo(StatusSessao.ANALISANDO);
    }

    // ---------- análise e resultado (task 13.4) ----------

    private static final String ANALISE_JSON = """
            {"nivelUrgencia": "MODERADO", "condicoes": ["gripe", "dengue"],
             "explicacaoEducativa": "Os sintomas podem estar relacionados a uma infecção viral.",
             "orientacoesGerais": "Repouso e hidratação.",
             "sinaisAlerta": ["Falta de ar", "Manchas vermelhas na pele"],
             "quandoBuscarAtendimento": "Procure uma unidade básica de saúde se não melhorar em 3 dias."}
            """;

    private long sessaoProntaParaAnalise(String token) throws Exception {
        when(geminiClient.gerarTexto(anyString())).thenReturn("Há quanto tempo você tem febre?");
        long sessaoId = iniciarTriagem(token);
        responder(token, sessaoId, "Desde ontem");
        mockMvc.perform(post("/api/triagem/" + sessaoId + "/encerrar").header("Authorization", token))
                .andExpect(status().isOk());
        return sessaoId;
    }

    @Test
    void analisaComBuscaPorPalavrasChaveEDevolveOResultadoCompleto() throws Exception {
        String token = cadastrarELogar();
        long sessaoId = sessaoProntaParaAnalise(token);
        when(geminiClient.gerarEmbeddings(anyList())).thenThrow(new IaIndisponivelException("embeddings fora"));
        when(geminiClient.gerarJson(anyString())).thenReturn(ANALISE_JSON);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessaoId").value(sessaoId))
                .andExpect(jsonPath("$.nivelUrgencia").value("MODERADO"))
                .andExpect(jsonPath("$.condicoes[0]").value("gripe"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.notaLimitacao").isNotEmpty())
                .andExpect(jsonPath("$.fontes[0].url").isNotEmpty())
                .andExpect(jsonPath("$.perguntasERespostas[0].pergunta").value("Há quanto tempo você tem febre?"))
                .andExpect(jsonPath("$.perguntasERespostas[0].resposta").value("Desde ontem"));

        assertThat(sessaoRepository.findById(sessaoId).orElseThrow().getStatus()).isEqualTo(StatusSessao.CONCLUIDA);

        mockMvc.perform(get("/api/triagem/" + sessaoId + "/resultado").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelUrgencia").value("MODERADO"))
                .andExpect(jsonPath("$.sinaisAlerta[1]").value("Manchas vermelhas na pele"));

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivelUrgencia").value("MODERADO"));
        verify(geminiClient, times(1)).gerarJson(anyString());
    }

    @Test
    void falhaDaIaNaAnaliseRetorna503EPermiteTentarDeNovo() throws Exception {
        String token = cadastrarELogar();
        long sessaoId = sessaoProntaParaAnalise(token);
        when(geminiClient.gerarJson(anyString())).thenThrow(new IaIndisponivelException("fora do ar"));

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", token))
                .andExpect(status().isServiceUnavailable());
        assertThat(sessaoRepository.findById(sessaoId).orElseThrow().getStatus()).isEqualTo(StatusSessao.ANALISANDO);

        doReturn(ANALISE_JSON).when(geminiClient).gerarJson(anyString());
        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void analisarOuVerResultadoForaDeHoraRetorna422() throws Exception {
        String token = cadastrarELogar();
        when(geminiClient.gerarTexto(anyString())).thenReturn("Há quanto tempo você tem febre?");
        long sessaoId = iniciarTriagem(token);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/triagem/" + sessaoId + "/resultado").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void resultadoDeOutroUsuarioRetorna404() throws Exception {
        String tokenDono = cadastrarELogar();
        String tokenOutro = cadastrarELogar();
        long sessaoId = sessaoProntaParaAnalise(tokenDono);

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/analisar").header("Authorization", tokenOutro))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/triagem/" + sessaoId + "/resultado").header("Authorization", tokenOutro))
                .andExpect(status().isNotFound());
    }
}
