package com.orientasaude.service;

import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import com.orientasaude.dto.response.IniciarTriagemResponse;
import com.orientasaude.dto.response.MensagemTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.exception.RecursoNaoEncontradoException;
import com.orientasaude.exception.TransicaoInvalidaException;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.repository.MensagemTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o fluxo da triagem (tasks 9.3, 11.1 e 11.3) com repositórios e entrevista simulados.
 */
class TriagemServiceTest {

    private final SessaoTriagemRepository sessaoRepository = mock(SessaoTriagemRepository.class);
    private final MensagemTriagemRepository mensagemRepository = mock(MensagemTriagemRepository.class);
    private final EntrevistaService entrevistaService = mock(EntrevistaService.class);
    private final TriagemService service =
            new TriagemService(sessaoRepository, mensagemRepository, entrevistaService);

    private final Usuario dono = Usuario.builder().id(1L).nome("Ana").email("ana@exemplo.com").build();
    private final Usuario outro = Usuario.builder().id(2L).nome("Beto").email("beto@exemplo.com").build();
    private SessaoTriagem sessao;

    @BeforeEach
    void preparar() {
        sessao = SessaoTriagem.builder()
                .id(10L)
                .usuario(dono)
                .idadePaciente(30)
                .sintomasIniciais("Febre e dor no corpo")
                .status(StatusSessao.EM_PROGRESSO)
                .build();
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoTriagem.class))).thenAnswer(inv -> {
            SessaoTriagem salva = inv.getArgument(0);
            if (salva.getId() == null) {
                salva.setId(10L);
            }
            return salva;
        });
        when(mensagemRepository.save(any(MensagemTriagem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- iniciar (task 9.3) ----------

    @Test
    void iniciarCriaSessaoSanitizadaComMensagemDoSistemaEPrimeiraPergunta() {
        when(entrevistaService.gerarProximaPergunta(any()))
                .thenReturn(ResultadoEntrevista.novaPergunta("Há quanto tempo?"));

        IniciarTriagemResponse resposta = service.iniciar(
                new IniciarTriagemRequest("<b>Febre</b> alta; desde ontem", 30), dono);

        assertThat(resposta.getSessaoId()).isEqualTo(10L);
        assertThat(resposta.getPrimeiraPergunta()).isEqualTo("Há quanto tempo?");

        ArgumentCaptor<SessaoTriagem> sessaoSalva = ArgumentCaptor.forClass(SessaoTriagem.class);
        verify(sessaoRepository).save(sessaoSalva.capture());
        assertThat(sessaoSalva.getValue().getSintomasIniciais()).isEqualTo("Febre alta desde ontem");
        assertThat(sessaoSalva.getValue().getStatus()).isEqualTo(StatusSessao.EM_PROGRESSO);
        assertThat(sessaoSalva.getValue().getUsuario()).isEqualTo(dono);

        ArgumentCaptor<MensagemTriagem> mensagens = ArgumentCaptor.forClass(MensagemTriagem.class);
        verify(mensagemRepository, times(2)).save(mensagens.capture());
        assertThat(mensagens.getAllValues())
                .extracting(MensagemTriagem::getPapel, MensagemTriagem::getOrdemSequencia)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(PapelMensagem.SISTEMA, 1),
                        org.assertj.core.groups.Tuple.tuple(PapelMensagem.ASSISTENTE, 2));
    }

    @Test
    void iniciarComSintomasQueFicamCurtosDemaisAposSanitizarRetornaEntradaInvalida() {
        assertThatThrownBy(() -> service.iniciar(
                new IniciarTriagemRequest("<script>alert</script>", 30), dono))
                .isInstanceOf(EntradaInvalidaException.class);
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void iniciarPropagaFalhaDaIaParaDesfazerATransacao() {
        when(entrevistaService.gerarProximaPergunta(any())).thenThrow(new IaIndisponivelException("fora"));

        assertThatThrownBy(() -> service.iniciar(new IniciarTriagemRequest("Febre alta desde ontem", 30), dono))
                .isInstanceOf(IaIndisponivelException.class);
    }

    // ---------- enviarMensagem (task 11.1) ----------

    @Test
    void enviarMensagemGravaRespostaEProximaPerguntaNaOrdem() {
        when(mensagemRepository.countBySessaoId(10L)).thenReturn(2L, 3L);
        when(entrevistaService.gerarProximaPergunta(sessao))
                .thenReturn(ResultadoEntrevista.novaPergunta("A febre passa com antitérmico?"));

        MensagemTriagemResponse resposta =
                service.enviarMensagem(10L, new EnviarMensagemRequest("Dois dias <i>já</i>"), dono);

        assertThat(resposta.getPergunta()).isEqualTo("A febre passa com antitérmico?");
        assertThat(resposta.isCompleta()).isFalse();
        assertThat(resposta.getAviso()).isNull();

        ArgumentCaptor<MensagemTriagem> mensagens = ArgumentCaptor.forClass(MensagemTriagem.class);
        verify(mensagemRepository, times(2)).save(mensagens.capture());
        MensagemTriagem respostaUsuario = mensagens.getAllValues().get(0);
        MensagemTriagem pergunta = mensagens.getAllValues().get(1);
        assertThat(respostaUsuario.getPapel()).isEqualTo(PapelMensagem.USUARIO);
        assertThat(respostaUsuario.getConteudo()).isEqualTo("Dois dias já");
        assertThat(respostaUsuario.getOrdemSequencia()).isEqualTo(3);
        assertThat(pergunta.getPapel()).isEqualTo(PapelMensagem.ASSISTENTE);
        assertThat(pergunta.getOrdemSequencia()).isEqualTo(4);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.EM_PROGRESSO);
    }

    @Test
    void entrevistaCompletaMudaSessaoParaAnalisando() {
        when(mensagemRepository.countBySessaoId(10L)).thenReturn(6L);
        when(entrevistaService.gerarProximaPergunta(sessao)).thenReturn(ResultadoEntrevista.concluida());

        MensagemTriagemResponse resposta = service.enviarMensagem(10L, new EnviarMensagemRequest("Não"), dono);

        assertThat(resposta.isCompleta()).isTrue();
        assertThat(resposta.getPergunta()).isNull();
        assertThat(resposta.getAviso()).isNull();
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.ANALISANDO);
        assertThat(sessao.getMotivoEncerramento()).isEqualTo(MotivoEncerramento.SUFICIENTE);
        verify(mensagemRepository, times(1)).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusSessao.class, names = "EM_PROGRESSO", mode = EnumSource.Mode.EXCLUDE)
    void soAceitaMensagemEmSessaoEmProgresso(StatusSessao status) {
        sessao.setStatus(status);

        assertThatThrownBy(() -> service.enviarMensagem(10L, new EnviarMensagemRequest("Sim"), dono))
                .isInstanceOf(TransicaoInvalidaException.class);
        verify(mensagemRepository, never()).save(any());
    }

    @Test
    void respostaQueFicaVaziaAposSanitizarRetornaEntradaInvalida() {
        assertThatThrownBy(() -> service.enviarMensagem(10L, new EnviarMensagemRequest("<b></b>"), dono))
                .isInstanceOf(EntradaInvalidaException.class);
    }

    // ---------- isolamento de dados (task 11.2, Propriedade 1) ----------

    @Test
    void sessaoDeOutroUsuarioRetornaNaoEncontrada() {
        assertThatThrownBy(() -> service.enviarMensagem(10L, new EnviarMensagemRequest("Sim"), outro))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.encerrar(10L, outro))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(mensagemRepository, never()).save(any());
    }

    @Test
    void sessaoInexistenteRetornaNaoEncontrada() {
        when(sessaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.encerrar(99L, dono))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- falhas e encerramentos (task 11.3) ----------

    @Test
    void falhaDaIaAoGerarPerguntaEhPropagadaParaDesfazerARespostaGravada() {
        when(mensagemRepository.countBySessaoId(10L)).thenReturn(2L);
        when(entrevistaService.gerarProximaPergunta(sessao)).thenThrow(new IaIndisponivelException("timeout"));

        assertThatThrownBy(() -> service.enviarMensagem(10L, new EnviarMensagemRequest("Sim"), dono))
                .isInstanceOf(IaIndisponivelException.class);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.EM_PROGRESSO);
    }

    @Test
    void limiteDe10PerguntasEncerraComAvisoDeColetaInconclusiva() {
        when(mensagemRepository.countBySessaoId(10L)).thenReturn(20L);
        when(entrevistaService.gerarProximaPergunta(sessao)).thenReturn(ResultadoEntrevista.inconclusivaPorLimite());

        MensagemTriagemResponse resposta = service.enviarMensagem(10L, new EnviarMensagemRequest("Não"), dono);

        assertThat(resposta.isCompleta()).isTrue();
        assertThat(resposta.getAviso()).isEqualTo(TriagemService.AVISO_COLETA_INCONCLUSIVA);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.ANALISANDO);
        assertThat(sessao.getMotivoEncerramento()).isEqualTo(MotivoEncerramento.LIMITE_PERGUNTAS);
    }

    @Test
    void encerrarAntecipadamenteMudaParaAnalisandoComAviso() {
        MensagemTriagemResponse resposta = service.encerrar(10L, dono);

        assertThat(resposta.isCompleta()).isTrue();
        assertThat(resposta.getAviso()).isEqualTo(TriagemService.AVISO_ENCERRAMENTO_ANTECIPADO);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.ANALISANDO);
        assertThat(sessao.getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ANTECIPADO);
        verify(entrevistaService, never()).gerarProximaPergunta(any());
    }

    @Test
    void encerrarSessaoQueNaoEstaEmProgressoRetornaTransicaoInvalida() {
        sessao.setStatus(StatusSessao.ANALISANDO);

        assertThatThrownBy(() -> service.encerrar(10L, dono))
                .isInstanceOf(TransicaoInvalidaException.class);
    }
}
