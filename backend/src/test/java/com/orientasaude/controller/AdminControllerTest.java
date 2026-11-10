package com.orientasaude.controller;

import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.PapelUsuario;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do painel administrativo com MySQL real (Testcontainers).
 * As triagens do cenário ficam em 2025 para não se misturar com as dos outros testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    private static final String SINTOMAS_SECRETOS = "Sintoma confidencial de teste 9137";
    private static final String PERIODO = "?inicio=2025-03-01&fim=2025-03-03";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario paciente;

    @BeforeEach
    void limparCenario() {
        jdbcTemplate.update("""
                DELETE r FROM resultados_triagem r JOIN sessoes_triagem s ON s.id = r.sessao_id
                WHERE s.iniciada_em < '2026-01-01'""");
        jdbcTemplate.update("DELETE FROM sessoes_triagem WHERE iniciada_em < '2026-01-01'");
    }

    private Usuario criarUsuario(PapelUsuario papel) {
        return usuarioRepository.save(Usuario.builder()
                .nome("Nome Secreto Fulano")
                .email("admin-teste-" + UUID.randomUUID() + "@exemplo.com")
                .senhaHash("hash-qualquer")
                .papel(papel)
                .build());
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtTokenProvider.gerarToken(usuario.getId(), usuario.getEmail(), usuario.getPapel());
    }

    private void criarSessao(MotivoEncerramento motivo, NivelUrgencia nivel, LocalDateTime iniciadaEm) {
        SessaoTriagem sessao = SessaoTriagem.builder()
                .usuario(paciente)
                .sintomasIniciais(SINTOMAS_SECRETOS)
                .idadePaciente(40)
                .status(nivel != null ? StatusSessao.CONCLUIDA : StatusSessao.EM_PROGRESSO)
                .motivoEncerramento(motivo)
                .build();
        if (nivel != null) {
            sessao.setResultado(ResultadoTriagem.builder()
                    .sessao(sessao)
                    .nivelUrgencia(nivel)
                    .condicoesJson("[\"gripe\"]")
                    .explicacaoEducativa("Explicação")
                    .orientacoesGerais("Orientações")
                    .sinaisAlerta("[\"Falta de ar\"]")
                    .quandoBuscarAtendimento("Quando buscar")
                    .disclaimer("Aviso")
                    .build());
        }
        long id = sessaoRepository.save(sessao).getId();
        jdbcTemplate.update("UPDATE sessoes_triagem SET iniciada_em = ? WHERE id = ?", iniciadaEm, id);
    }

    private void montarCenario() {
        paciente = criarUsuario(PapelUsuario.USUARIO);
        criarSessao(MotivoEncerramento.SUFICIENTE, NivelUrgencia.LEVE, LocalDateTime.of(2025, 3, 1, 9, 0));
        criarSessao(MotivoEncerramento.SUFICIENTE, NivelUrgencia.LEVE, LocalDateTime.of(2025, 3, 1, 23, 59));
        criarSessao(MotivoEncerramento.ANTECIPADO, NivelUrgencia.URGENTE, LocalDateTime.of(2025, 3, 3, 8, 0));
        criarSessao(null, null, LocalDateTime.of(2025, 3, 3, 10, 0));
        // Fora do período: não pode entrar na conta
        criarSessao(MotivoEncerramento.SUFICIENTE, NivelUrgencia.EMERGENCIA, LocalDateTime.of(2025, 3, 4, 0, 0));
    }

    @Test
    void adminVeOsIndicadoresAgregadosDoPeriodo() throws Exception {
        montarCenario();
        Usuario admin = criarUsuario(PapelUsuario.ADMIN);

        mockMvc.perform(get("/api/admin/indicadores" + PERIODO).header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2025-03-01"))
                .andExpect(jsonPath("$.fim").value("2025-03-03"))
                .andExpect(jsonPath("$.agrupamento").value("DIA"))
                .andExpect(jsonPath("$.totalTriagens").value(4))
                .andExpect(jsonPath("$.triagensPorPeriodo.length()").value(3))
                .andExpect(jsonPath("$.triagensPorPeriodo[0].total").value(2))
                .andExpect(jsonPath("$.triagensPorPeriodo[1].total").value(0))
                .andExpect(jsonPath("$.triagensPorPeriodo[2].total").value(2))
                .andExpect(jsonPath("$.distribuicaoUrgencia.LEVE").value(2))
                .andExpect(jsonPath("$.distribuicaoUrgencia.MODERADO").value(0))
                .andExpect(jsonPath("$.distribuicaoUrgencia.URGENTE").value(1))
                .andExpect(jsonPath("$.distribuicaoUrgencia.EMERGENCIA").value(0))
                .andExpect(jsonPath("$.triagensFinalizadas").value(3))
                .andExpect(jsonPath("$.encerradasAntecipadamente").value(1))
                .andExpect(jsonPath("$.taxaEncerramentoAntecipado").value(33.3));
    }

    @Test
    void respostaDoPainelNaoTemNenhumDadoPessoalNemDeSaude() throws Exception {
        montarCenario();
        Usuario admin = criarUsuario(PapelUsuario.ADMIN);

        String corpo = mockMvc.perform(get("/api/admin/indicadores" + PERIODO).header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .doesNotContain(SINTOMAS_SECRETOS)
                .doesNotContain("Nome Secreto")
                .doesNotContain(paciente.getEmail())
                .doesNotContain("@")
                .doesNotContain("usuario")
                .doesNotContain("sintoma");
    }

    @Test
    void usuarioComumRecebe403EFicaNaAuditoria() throws Exception {
        Usuario comum = criarUsuario(PapelUsuario.USUARIO);

        mockMvc.perform(get("/api/admin/indicadores").header("Authorization", token(comum)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para acessar este recurso."));

        assertThat(logAuditoriaRepository.findByUsuarioId(comum.getId()))
                .extracting("tipoEvento")
                .containsExactly(TipoEventoAuditoria.PERMISSAO_NEGADA);
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/admin/indicadores")).andExpect(status().isUnauthorized());
    }

    @Test
    void periodoInvalidoRecebe400() throws Exception {
        Usuario admin = criarUsuario(PapelUsuario.ADMIN);

        mockMvc.perform(get("/api/admin/indicadores?inicio=2025-03-10&fim=2025-03-01")
                        .header("Authorization", token(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data final não pode ser anterior à data inicial"));
    }

    @Test
    void contaCriadaPeloCadastroPublicoNasceComoUsuario() throws Exception {
        String email = "publico-" + UUID.randomUUID() + "@exemplo.com";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/registro")
                        .contentType("application/json")
                        .content("{\"nome\": \"Maria Silva\", \"email\": \"" + email
                                + "\", \"senha\": \"senhaSegura123\", \"consentimento\": true, \"papel\": \"ADMIN\"}"))
                .andExpect(status().isCreated());

        assertThat(usuarioRepository.findByEmail(email).orElseThrow().getPapel()).isEqualTo(PapelUsuario.USUARIO);
    }
}
