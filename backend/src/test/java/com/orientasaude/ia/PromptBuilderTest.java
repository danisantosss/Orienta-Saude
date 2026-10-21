package com.orientasaude.ia;

import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.PapelMensagem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private SessaoTriagem criarSessao() {
        Usuario usuario = Usuario.builder()
                .id(4242L)
                .nome("Maria Joaquina")
                .email("maria.joaquina@exemplo.com")
                .build();
        return SessaoTriagem.builder()
                .id(77L)
                .usuario(usuario)
                .idadePaciente(34)
                .sintomasIniciais("Dor de cabeça forte há dois dias")
                .build();
    }

    private MensagemTriagem mensagem(PapelMensagem papel, String conteudo, int ordem) {
        return MensagemTriagem.builder().papel(papel).conteudo(conteudo).ordemSequencia(ordem).build();
    }

    @Test
    void promptContemIdadeSintomasEHistorico() {
        List<MensagemTriagem> mensagens = List.of(
                mensagem(PapelMensagem.SISTEMA, "Dor de cabeça forte há dois dias", 1),
                mensagem(PapelMensagem.ASSISTENTE, "Você tem febre?", 2),
                mensagem(PapelMensagem.USUARIO, "Sim, 38 graus", 3));

        String prompt = promptBuilder.montarPromptEntrevista(criarSessao(), mensagens);

        assertThat(prompt)
                .contains("Paciente: 34 anos")
                .contains("Sintomas iniciais: Dor de cabeça forte há dois dias")
                .contains("Pergunta: Você tem febre?")
                .contains("Resposta: Sim, 38 graus")
                .contains("Perguntas já feitas: 1 (mínimo 2, máximo 10)");
    }

    @Test
    void promptNuncaContemDadosQueIdentificamOUsuario() {
        List<MensagemTriagem> mensagens = List.of(
                mensagem(PapelMensagem.ASSISTENTE, "Você tem febre?", 2),
                mensagem(PapelMensagem.USUARIO, "Não", 3));

        String prompt = promptBuilder.montarPromptEntrevista(criarSessao(), mensagens);

        assertThat(prompt)
                .doesNotContain("Maria")
                .doesNotContain("Joaquina")
                .doesNotContain("maria.joaquina@exemplo.com")
                .doesNotContain("4242")
                .doesNotContain("77");
    }

    @Test
    void antesDoMinimoOPromptProibeEncerrarAEntrevista() {
        String prompt = promptBuilder.montarPromptEntrevista(criarSessao(), List.of());

        assertThat(prompt)
                .contains("Perguntas já feitas: 0")
                .contains("Ainda NÃO encerre a entrevista")
                .doesNotContain("responda apenas: " + PromptBuilder.MARCADOR_ENTREVISTA_COMPLETA);
    }

    @Test
    void semHistoricoIndicaQueEAPrimeiraPergunta() {
        String prompt = promptBuilder.montarPromptEntrevista(criarSessao(), List.of());

        assertThat(prompt).contains("Histórico da conversa: (nenhuma pergunta feita ainda)");
    }

    @Test
    void aposOMinimoOPromptPermiteEncerrarComOMarcador() {
        List<MensagemTriagem> mensagens = List.of(
                mensagem(PapelMensagem.ASSISTENTE, "Você tem febre?", 2),
                mensagem(PapelMensagem.USUARIO, "Sim", 3),
                mensagem(PapelMensagem.ASSISTENTE, "Há quanto tempo?", 4),
                mensagem(PapelMensagem.USUARIO, "Dois dias", 5));

        String prompt = promptBuilder.montarPromptEntrevista(criarSessao(), mensagens);

        assertThat(prompt)
                .contains("Perguntas já feitas: 2")
                .contains("responda apenas: " + PromptBuilder.MARCADOR_ENTREVISTA_COMPLETA)
                .doesNotContain("Ainda NÃO encerre a entrevista");
    }

    // ---------- análise final ----------

    private List<MensagemTriagem> entrevistaCompleta() {
        return List.of(
                mensagem(PapelMensagem.SISTEMA, "Dor de cabeça forte há dois dias", 1),
                mensagem(PapelMensagem.ASSISTENTE, "Você tem febre?", 2),
                mensagem(PapelMensagem.USUARIO, "Sim, 38 graus", 3));
    }

    @Test
    void promptDeAnaliseTrazEntrevistaContextoEFormatoJson() {
        String prompt = promptBuilder.montarPromptAnalise(criarSessao(), entrevistaCompleta(), "[febre-001] Fonte: Ministério da Saúde");

        assertThat(prompt)
                .contains("Paciente: 34 anos")
                .contains("Sintomas iniciais: Dor de cabeça forte há dois dias")
                .contains("Pergunta: Você tem febre?")
                .contains("Resposta: Sim, 38 graus")
                .contains("[febre-001] Fonte: Ministério da Saúde")
                .contains("\"nivelUrgencia\"")
                .contains("\"quandoBuscarAtendimento\"")
                .contains("EMERGENCIA")
                .contains("192")
                .contains("NÃO prescreva medicamentos");
    }

    @Test
    void promptDeAnaliseNuncaContemDadosQueIdentificamOUsuario() {
        String prompt = promptBuilder.montarPromptAnalise(criarSessao(), entrevistaCompleta(), "contexto");

        assertThat(prompt)
                .doesNotContain("Maria")
                .doesNotContain("Joaquina")
                .doesNotContain("maria.joaquina@exemplo.com")
                .doesNotContain("4242")
                .doesNotContain("77");
    }

    @Test
    void promptDeReforcoListaOsTermosProibidosEPedeParaReescrever() {
        String prompt = promptBuilder.montarPromptReforco("PROMPT BASE", List.of("você tem", "paracetamol"));

        assertThat(prompt)
                .startsWith("PROMPT BASE")
                .contains("você tem")
                .contains("paracetamol")
                .contains("Reescreva");
    }
}
