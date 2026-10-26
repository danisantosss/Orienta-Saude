package com.orientasaude.controller;

import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.StatusSessao;
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do histórico com MySQL real (Testcontainers).
 * As sessões são gravadas direto no banco, com datas controladas pelo teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoricoTriagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Usuario usuario;
    private String token;

    @BeforeEach
    void preparar() {
        usuario = criarUsuario();
        token = "Bearer " + jwtTokenProvider.gerarToken(usuario.getId(), usuario.getEmail());
    }

    private Usuario criarUsuario() {
        return usuarioRepository.save(Usuario.builder()
                .nome("Pessoa Teste")
                .email("historico-" + UUID.randomUUID() + "@exemplo.com")
                .senhaHash("hash-qualquer")
                .build());
    }

    private long criarSessaoConcluida(Usuario dono, String sintomas, NivelUrgencia nivel, LocalDateTime iniciadaEm) {
        SessaoTriagem sessao = SessaoTriagem.builder()
                .usuario(dono)
                .sintomasIniciais(sintomas)
                .idadePaciente(30)
                .status(StatusSessao.CONCLUIDA)
                .finalizadaEm(iniciadaEm.plusMinutes(5))
                .build();
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
        long id = sessaoRepository.save(sessao).getId();
        // iniciada_em é preenchida pelo @PrePersist; aqui a data é trocada para montar o cenário
        jdbcTemplate.update("UPDATE sessoes_triagem SET iniciada_em = ? WHERE id = ?", iniciadaEm, id);
        return id;
    }

    @Test
    void listaSoAsSessoesDoUsuarioDaMaisRecenteParaAMaisAntiga() throws Exception {
        long antiga = criarSessaoConcluida(usuario, "Dor de garganta e tosse seca", NivelUrgencia.LEVE,
                LocalDateTime.of(2026, 3, 30, 12, 22));
        long recente = criarSessaoConcluida(usuario, "Febre, dor de cabeça e tosse", NivelUrgencia.MODERADO,
                LocalDateTime.of(2026, 6, 12, 17, 30));
        criarSessaoConcluida(criarUsuario(), "Sessão de outra pessoa", NivelUrgencia.URGENTE,
                LocalDateTime.of(2026, 5, 1, 8, 0));

        mockMvc.perform(get("/api/triagem/historico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].id").value(contains((int) recente, (int) antiga)))
                .andExpect(jsonPath("$.itens[0].sintomasIniciais").value("Febre, dor de cabeça e tosse"))
                .andExpect(jsonPath("$.itens[0].nivelUrgencia").value("MODERADO"))
                .andExpect(jsonPath("$.itens[0].status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.itens[0].iniciadaEm").value("2026-06-12T17:30:00"))
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20));
    }

    @Test
    void filtraPorNivelDeUrgencia() throws Exception {
        criarSessaoConcluida(usuario, "Coceira nos olhos e espirros", NivelUrgencia.LEVE, LocalDateTime.of(2026, 3, 30, 15, 35));
        long urgente = criarSessaoConcluida(usuario, "Náusea e diarreia há três dias", NivelUrgencia.URGENTE,
                LocalDateTime.of(2026, 4, 17, 9, 30));

        mockMvc.perform(get("/api/triagem/historico").param("nivelUrgencia", "URGENTE").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].id").value(urgente));
    }

    @Test
    void filtraPorPeriodoIncluindoODiaFinalInteiro() throws Exception {
        criarSessaoConcluida(usuario, "Antes do período escolhido", NivelUrgencia.LEVE, LocalDateTime.of(2026, 3, 31, 23, 59));
        long inicioDoPeriodo = criarSessaoConcluida(usuario, "No primeiro dia do período", NivelUrgencia.LEVE,
                LocalDateTime.of(2026, 4, 1, 0, 0));
        long fimDoPeriodo = criarSessaoConcluida(usuario, "No último dia, à noite", NivelUrgencia.LEVE,
                LocalDateTime.of(2026, 4, 30, 23, 30));
        criarSessaoConcluida(usuario, "Depois do período escolhido", NivelUrgencia.LEVE, LocalDateTime.of(2026, 5, 1, 0, 0));

        mockMvc.perform(get("/api/triagem/historico")
                        .param("inicio", "2026-04-01")
                        .param("fim", "2026-04-30")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].id").value(contains((int) fimDoPeriodo, (int) inicioDoPeriodo)));
    }

    @Test
    void paginaOResultado() throws Exception {
        for (int dia = 1; dia <= 5; dia++) {
            criarSessaoConcluida(usuario, "Sintomas do dia " + dia + " de abril", NivelUrgencia.LEVE,
                    LocalDateTime.of(2026, 4, dia, 10, 0));
        }

        mockMvc.perform(get("/api/triagem/historico")
                        .param("pagina", "1")
                        .param("tamanho", "2")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].sintomasIniciais").value("Sintomas do dia 3 de abril"))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalItens").value(5))
                .andExpect(jsonPath("$.totalPaginas").value(3));
    }

    @Test
    void sessaoEmAndamentoApareceSemNivel() throws Exception {
        sessaoRepository.save(SessaoTriagem.builder()
                .usuario(usuario)
                .sintomasIniciais("Entrevista ainda não terminada")
                .idadePaciente(40)
                .build());

        mockMvc.perform(get("/api/triagem/historico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].status").value("EM_PROGRESSO"))
                .andExpect(jsonPath("$.itens[0].nivelUrgencia").doesNotExist());
    }

    @Test
    void historicoVazioDevolveListaVazia() throws Exception {
        mockMvc.perform(get("/api/triagem/historico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.totalItens").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void filtrosInvalidosRetornam400() throws Exception {
        mockMvc.perform(get("/api/triagem/historico").param("nivelUrgencia", "GRAVE").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro nivelUrgencia"));
        mockMvc.perform(get("/api/triagem/historico").param("inicio", "30/04/2026").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/triagem/historico")
                        .param("inicio", "2026-05-01")
                        .param("fim", "2026-04-01")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data inicial não pode ser depois da data final"));
    }

    @Test
    void semTokenNaoAcessaOHistorico() throws Exception {
        mockMvc.perform(get("/api/triagem/historico"))
                .andExpect(status().isForbidden());
    }
}
