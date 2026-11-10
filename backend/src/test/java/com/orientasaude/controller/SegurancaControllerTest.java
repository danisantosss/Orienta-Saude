package com.orientasaude.controller;

import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração das proteções da API: limite de requisições, bloqueio de login, CORS,
 * respostas 401 e registros de auditoria gravados no MySQL real (Testcontainers).
 * Cada teste usa um IP próprio, porque limite e bloqueio são contados por IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.seguranca.limite-requisicoes-por-minuto=8",
    "app.cors.origens-permitidas=http://localhost:3000"
})
class SegurancaControllerTest {

    private static final String SENHA = "senhaSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    private Usuario criarUsuario() {
        return usuarioRepository.save(Usuario.builder()
                .nome("Pessoa Teste")
                .email("seguranca-" + UUID.randomUUID() + "@exemplo.com")
                .senhaHash(passwordEncoder.encode(SENHA))
                .build());
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtTokenProvider.gerarToken(usuario.getId(), usuario.getEmail(), usuario.getPapel());
    }

    private MvcResult logar(String email, String senha, String endereco) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(ip(endereco))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}"))
                .andReturn();
    }

    private long contarEventos(TipoEventoAuditoria tipo, String endereco) {
        return logAuditoriaRepository.findByTipoEventoAndIp(tipo, endereco).size();
    }

    @Test
    void ipQuePassaDoLimitePorMinutoRecebe429() throws Exception {
        // 17 requisições: mesmo que o minuto vire no meio, uma das janelas passa de 8
        List<MvcResult> respostas = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            respostas.add(mockMvc.perform(get("/api/triagem/historico").with(ip("10.1.0.1"))).andReturn());
        }

        MvcResult recusada = respostas.stream()
                .filter(resposta -> resposta.getResponse().getStatus() == 429)
                .findFirst()
                .orElseThrow();
        assertThat(recusada.getResponse().getHeader("Retry-After")).isNotBlank();
        assertThat(recusada.getResponse().getContentAsString()).contains("Aguarde um minuto");
        assertThat(contarEventos(TipoEventoAuditoria.LIMITE_EXCEDIDO, "10.1.0.1")).isPositive();
        // Outro IP continua sendo atendido normalmente
        mockMvc.perform(get("/api/triagem/historico").with(ip("10.1.0.2"))).andExpect(status().isUnauthorized());
    }

    @Test
    void cincoSenhasErradasBloqueiamOLoginDoIp() throws Exception {
        Usuario usuario = criarUsuario();

        for (int i = 0; i < 5; i++) {
            assertThat(logar(usuario.getEmail(), "senhaErrada99", "10.2.0.1").getResponse().getStatus()).isEqualTo(401);
        }

        // Bloqueado: nem a senha certa entra
        mockMvc.perform(post("/api/auth/login").with(ip("10.2.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + usuario.getEmail() + "\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("8[5-9]\\d|900")))
                .andExpect(jsonPath("$.mensagem").value(
                        "Muitas tentativas de login sem sucesso. Tente novamente em 15 minutos."));

        assertThat(contarEventos(TipoEventoAuditoria.LOGIN_FALHO, "10.2.0.1")).isEqualTo(5);
        assertThat(contarEventos(TipoEventoAuditoria.LOGIN_BLOQUEADO, "10.2.0.1")).isOne();
        assertThat(logAuditoriaRepository.findByTipoEventoAndIp(TipoEventoAuditoria.LOGIN_FALHO, "10.2.0.1"))
                .allMatch(registro -> usuario.getId().equals(registro.getUsuarioId()));

        // O bloqueio é do IP, não da conta: de outro lugar o dono entra
        assertThat(logar(usuario.getEmail(), SENHA, "10.2.0.2").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void corsLiberaSoAOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/api/auth/login").with(ip("10.3.0.1"))
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(options("/api/auth/login").with(ip("10.3.0.1"))
                        .header("Origin", "https://site-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void semTokenRecebe401NoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/api/usuario/perfil").with(ip("10.4.0.1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar."))
                .andExpect(jsonPath("$.caminho").value("/api/usuario/perfil"));

        assertThat(contarEventos(TipoEventoAuditoria.TOKEN_INVALIDO, "10.4.0.1")).isZero();
    }

    @Test
    void tokenAdulteradoRecebe401EFicaNaAuditoria() throws Exception {
        mockMvc.perform(get("/api/usuario/perfil").with(ip("10.5.0.1"))
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.adulterado.assinatura"))
                .andExpect(status().isUnauthorized());

        assertThat(contarEventos(TipoEventoAuditoria.TOKEN_INVALIDO, "10.5.0.1")).isOne();
    }

    @Test
    void tentativaDeMexerNaTriagemDeOutroFicaNaAuditoriaMesmoComORollback() throws Exception {
        Usuario dono = criarUsuario();
        Usuario intruso = criarUsuario();
        long sessaoId = sessaoRepository.save(SessaoTriagem.builder()
                .usuario(dono)
                .sintomasIniciais("Dor de cabeça forte desde ontem")
                .idadePaciente(30)
                .status(StatusSessao.EM_PROGRESSO)
                .build()).getId();

        mockMvc.perform(post("/api/triagem/" + sessaoId + "/mensagem").with(ip("10.6.0.1"))
                        .header("Authorization", token(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Sim\"}"))
                .andExpect(status().isNotFound());

        assertThat(logAuditoriaRepository.findByTipoEventoAndIp(TipoEventoAuditoria.ACESSO_NEGADO, "10.6.0.1"))
                .singleElement()
                .satisfies(registro -> assertThat(registro.getUsuarioId()).isEqualTo(intruso.getId()));
    }
}
