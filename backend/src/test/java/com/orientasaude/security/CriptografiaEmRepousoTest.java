package com.orientasaude.security;

import com.orientasaude.config.AjusteColunasCriptografadas;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere, no MySQL real, que os dados de saúde ficam cifrados nas colunas
 * e voltam legíveis quando lidos pelo JPA.
 */
@SpringBootTest
@ActiveProfiles("test")
class CriptografiaEmRepousoTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoTriagemRepository sessaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private AjusteColunasCriptografadas ajusteColunas;

    private long salvarSessaoCompleta() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Pessoa Teste")
                .email("cripto-" + UUID.randomUUID() + "@exemplo.com")
                .senhaHash("hash-qualquer")
                .build());
        SessaoTriagem sessao = SessaoTriagem.builder()
                .usuario(usuario)
                .sintomasIniciais("Dor no peito que irradia para o braço")
                .idadePaciente(58)
                .build();
        sessao.getMensagens().add(MensagemTriagem.builder()
                .sessao(sessao).papel(PapelMensagem.USUARIO).conteudo("Começou há duas horas").ordemSequencia(1).build());
        sessao.setResultado(ResultadoTriagem.builder()
                .sessao(sessao)
                .nivelUrgencia(NivelUrgencia.EMERGENCIA)
                .condicoesJson("[\"infarto\"]")
                .explicacaoEducativa("Explicação sobre infarto")
                .orientacoesGerais("Ligue para o SAMU")
                .sinaisAlerta("[\"Suor frio\"]")
                .quandoBuscarAtendimento("Agora")
                .notaLimitacao("Nota sobre limitação")
                .disclaimer("Aviso")
                .build());
        return sessaoRepository.save(sessao).getId();
    }

    @Test
    void dadosDeSaudeFicamCifradosNoBancoELegiveisPeloJpa() {
        long id = salvarSessaoCompleta();

        String sintomasNoBanco = jdbcTemplate.queryForObject(
                "SELECT sintomas_iniciais FROM sessoes_triagem WHERE id = ?", String.class, id);
        String mensagemNoBanco = jdbcTemplate.queryForObject(
                "SELECT conteudo FROM mensagens_triagem WHERE sessao_id = ?", String.class, id);
        Map<String, Object> resultadoNoBanco = jdbcTemplate.queryForMap(
                "SELECT condicoes_json, explicacao_educativa, orientacoes_gerais, sinais_alerta, "
                        + "quando_buscar_atendimento, nota_limitacao FROM resultados_triagem WHERE sessao_id = ?", id);

        assertThat(sintomasNoBanco).startsWith("enc:v1:").doesNotContain("peito");
        assertThat(mensagemNoBanco).startsWith("enc:v1:").doesNotContain("horas");
        assertThat(resultadoNoBanco.values()).allSatisfy(valor -> assertThat((String) valor).startsWith("enc:v1:"));

        transacao.executeWithoutResult(status -> {
            SessaoTriagem lida = sessaoRepository.findById(id).orElseThrow();
            assertThat(lida.getSintomasIniciais()).isEqualTo("Dor no peito que irradia para o braço");
            assertThat(lida.getMensagens().get(0).getConteudo()).isEqualTo("Começou há duas horas");
            assertThat(lida.getResultado().getCondicoesJson()).isEqualTo("[\"infarto\"]");
            assertThat(lida.getResultado().getNotaLimitacao()).isEqualTo("Nota sobre limitação");
        });
    }

    @Test
    void sessaoGravadaAntesDaCriptografiaContinuaLegivel() {
        long id = salvarSessaoCompleta();
        jdbcTemplate.update("UPDATE sessoes_triagem SET sintomas_iniciais = ? WHERE id = ?",
                "Sintomas antigos gravados em texto puro", id);

        SessaoTriagem lida = sessaoRepository.findById(id).orElseThrow();

        assertThat(lida.getSintomasIniciais()).isEqualTo("Sintomas antigos gravados em texto puro");
    }

    @Test
    void ajusteTrocaColunasAntigasParaTextoMantendoAObrigatoriedade() throws Exception {
        jdbcTemplate.execute("ALTER TABLE sessoes_triagem MODIFY sintomas_iniciais VARCHAR(1000) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE resultados_triagem MODIFY nota_limitacao VARCHAR(500) NULL");

        ajusteColunas.run(null);

        assertThat(tipoDaColuna("sessoes_triagem", "sintomas_iniciais")).containsEntry("DATA_TYPE", "text")
                .containsEntry("IS_NULLABLE", "NO");
        assertThat(tipoDaColuna("resultados_triagem", "nota_limitacao")).containsEntry("DATA_TYPE", "text")
                .containsEntry("IS_NULLABLE", "YES");
    }

    private Map<String, Object> tipoDaColuna(String tabela, String coluna) {
        return jdbcTemplate.queryForMap("""
                SELECT DATA_TYPE, IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, tabela, coluna);
    }
}
