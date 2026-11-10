package com.orientasaude.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do perfil e da exclusão de conta com MySQL real (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    private static final String SENHA = "senhaSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String email;
    private String token;

    @BeforeEach
    void cadastrarELogar() throws Exception {
        email = "perfil-" + UUID.randomUUID() + "@exemplo.com";
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Maria Silva\", \"email\": \"" + email
                                + "\", \"senha\": \"" + SENHA + "\", \"consentimento\": true}"))
                .andExpect(status().isCreated());
        token = "Bearer " + logar().get("token").asText();
    }

    private JsonNode logar() throws Exception {
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"" + SENHA + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    private long criarSessaoComMensagemEResultado(Usuario dono) {
        SessaoTriagem sessao = SessaoTriagem.builder()
                .usuario(dono)
                .sintomasIniciais("Febre alta e dor no corpo")
                .idadePaciente(30)
                .build();
        sessao.getMensagens().add(MensagemTriagem.builder()
                .sessao(sessao).papel(PapelMensagem.USUARIO).conteudo("Desde ontem").ordemSequencia(1).build());
        sessao.setResultado(ResultadoTriagem.builder()
                .sessao(sessao)
                .nivelUrgencia(NivelUrgencia.MODERADO)
                .condicoesJson("[\"gripe\"]")
                .explicacaoEducativa("Explicação")
                .orientacoesGerais("Orientações")
                .sinaisAlerta("[\"Falta de ar\"]")
                .quandoBuscarAtendimento("Quando buscar")
                .disclaimer("Aviso")
                .build());
        return sessaoRepository.save(sessao).getId();
    }

    private int contar(String sql, Object... parametros) {
        return jdbcTemplate.queryForObject(sql, Integer.class, parametros);
    }

    @Test
    void perfilMostraDadosDeCadastroEDataDoConsentimento() throws Exception {
        mockMvc.perform(get("/api/usuario/perfil").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                .andExpect(jsonPath("$.consentimentoEm").isNotEmpty())
                .andExpect(jsonPath("$.versaoPolitica").value("1.0"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void perfilExigeToken() throws Exception {
        mockMvc.perform(get("/api/usuario/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    void excluirContaComSenhaErradaMantemTudo() throws Exception {
        mockMvc.perform(delete("/api/usuario/conta")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\": \"senhaErrada99\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Senha incorreta"));

        assertThat(usuarioRepository.findByEmail(email)).isPresent();
    }

    @Test
    void excluirContaSemSenhaEhRecusado() throws Exception {
        mockMvc.perform(delete("/api/usuario/conta")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void excluirContaApagaUsuarioSessoesMensagensEResultadosEBloqueiaOAcesso() throws Exception {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        long sessaoId = criarSessaoComMensagemEResultado(usuario);
        long sessaoDeOutraPessoa = criarSessaoComMensagemEResultado(usuarioRepository.save(Usuario.builder()
                .nome("Outra Pessoa").email("outra-" + UUID.randomUUID() + "@exemplo.com").senhaHash("hash").build()));

        mockMvc.perform(delete("/api/usuario/conta")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value(
                        "Recebemos sua solicitação. Sua conta e todos os seus dados foram excluídos."));

        assertThat(usuarioRepository.findByEmail(email)).isEmpty();
        assertThat(contar("SELECT COUNT(*) FROM sessoes_triagem WHERE id = ?", sessaoId)).isZero();
        assertThat(contar("SELECT COUNT(*) FROM mensagens_triagem WHERE sessao_id = ?", sessaoId)).isZero();
        assertThat(contar("SELECT COUNT(*) FROM resultados_triagem WHERE sessao_id = ?", sessaoId)).isZero();
        assertThat(contar("SELECT COUNT(*) FROM sessoes_triagem WHERE id = ?", sessaoDeOutraPessoa)).isOne();

        // O token antigo deixa de valer e o login com as mesmas credenciais é recusado
        mockMvc.perform(get("/api/usuario/perfil").header("Authorization", token)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
