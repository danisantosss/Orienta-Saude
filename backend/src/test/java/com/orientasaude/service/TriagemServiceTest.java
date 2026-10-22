package com.orientasaude.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import com.orientasaude.dto.response.IniciarTriagemResponse;
import com.orientasaude.dto.response.MensagemTriagemResponse;
import com.orientasaude.dto.response.ResultadoTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.exception.RecursoNaoEncontradoException;
import com.orientasaude.exception.TransicaoInvalidaException;
import com.orientasaude.ia.AnaliseGerada;
import com.orientasaude.ia.ValidadorEtico;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.rag.BaseConhecimento;
import com.orientasaude.rag.ContextoRag;
import com.orientasaude.rag.RagService;
import com.orientasaude.repository.MensagemTriagemRepository;
import com.orientasaude.repository.ResultadoTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o fluxo da triagem com repositórios, entrevista,
 * RAG e análise simulados.
 */
class TriagemServiceTest {

    private final SessaoTriagemRepository sessaoRepository = mock(SessaoTriagemRepository.class);
    private final MensagemTriagemRepository mensagemRepository = mock(MensagemTriagemRepository.class);
    private final EntrevistaService entrevistaService = mock(EntrevistaService.class);
    private final ResultadoTriagemRepository resultadoRepository = mock(ResultadoTriagemRepository.class);
    private final RagService ragService = mock(RagService.class);
    private final AnaliseService analiseService = mock(AnaliseService.class);
    private final BaseConhecimento baseConhecimento = criarBaseConhecimento();
    private final TriagemService service = new TriagemService(sessaoRepository, mensagemRepository,
            resultadoRepository, entrevistaService, ragService, analiseService, baseConhecimento, new ObjectMapper());

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

    // ---------- iniciar ----------

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

    // ---------- enviarMensagem ----------

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

    // ---------- isolamento de dados ----------

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

    // ---------- falhas e encerramentos ----------

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

    // ---------- analisar e obterResultado ----------

    private static BaseConhecimento criarBaseConhecimento() {
        RagConfig config = new RagConfig();
        config.setPadraoDocumentos("classpath:knowledge-base/*.json");
        return new BaseConhecimento(config, new ObjectMapper());
    }

    private final AnaliseGerada analise = new AnaliseGerada(NivelUrgencia.MODERADO, List.of("gripe", "dengue"),
            "Os sintomas podem estar relacionados a uma infecção viral.", "Repouso e hidratação.",
            List.of("Falta de ar", "Manchas vermelhas"), "Procure uma unidade de saúde se não melhorar em 3 dias.");

    private void prepararAnalise(MotivoEncerramento motivo, ContextoRag contexto) {
        sessao.setStatus(StatusSessao.ANALISANDO);
        sessao.setMotivoEncerramento(motivo);
        when(mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(10L)).thenReturn(List.of(
                MensagemTriagem.builder().papel(PapelMensagem.SISTEMA).conteudo("Febre e dor no corpo").ordemSequencia(1).build(),
                MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE).conteudo("Há quanto tempo?").ordemSequencia(2).build(),
                MensagemTriagem.builder().papel(PapelMensagem.USUARIO).conteudo("Dois dias").ordemSequencia(3).build(),
                MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE).conteudo("Tem manchas na pele?").ordemSequencia(4).build()));
        when(ragService.recuperar(any(), anyList())).thenReturn(contexto);
        when(analiseService.analisar(any(), anyList(), any())).thenReturn(analise);
    }

    private ResultadoTriagem resultadoSalvo() {
        ArgumentCaptor<SessaoTriagem> salva = ArgumentCaptor.forClass(SessaoTriagem.class);
        verify(sessaoRepository).save(salva.capture());
        return salva.getValue().getResultado();
    }

    @Test
    void analisarPersisteResultadoComDisclaimerEConcluiASessao() {
        prepararAnalise(MotivoEncerramento.SUFICIENTE,
                new ContextoRag("contexto", List.of("febre-001", "dor-no-corpo-001", "gripe-001"), null));

        ResultadoTriagemResponse resposta = service.analisar(10L, dono);

        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.CONCLUIDA);
        assertThat(sessao.getFinalizadaEm()).isNotNull();
        ResultadoTriagem resultado = resultadoSalvo();
        assertThat(resultado.getSessao()).isSameAs(sessao);
        assertThat(resultado.getDisclaimer()).isEqualTo(ValidadorEtico.DISCLAIMER);
        assertThat(resultado.getNivelUrgencia()).isEqualTo(NivelUrgencia.MODERADO);
        assertThat(resultado.getCondicoesJson()).isEqualTo("[\"gripe\",\"dengue\"]");
        assertThat(resultado.getSinaisAlerta()).isEqualTo("[\"Falta de ar\",\"Manchas vermelhas\"]");
        assertThat(resultado.getDocumentosRagUsados()).isEqualTo("[\"febre-001\",\"dor-no-corpo-001\",\"gripe-001\"]");
        assertThat(resultado.getNotaLimitacao()).isNull();

        assertThat(resposta.getSessaoId()).isEqualTo(10L);
        assertThat(resposta.getDisclaimer()).isEqualTo(ValidadorEtico.DISCLAIMER);
        assertThat(resposta.getCondicoes()).containsExactly("gripe", "dengue");
        assertThat(resposta.getSinaisAlerta()).containsExactly("Falta de ar", "Manchas vermelhas");
        assertThat(resposta.getFontes()).hasSize(3)
                .allSatisfy(fonte -> assertThat(fonte.url()).startsWith("https://"));
        assertThat(resposta.getPerguntasERespostas()).hasSize(2);
        assertThat(resposta.getPerguntasERespostas().get(0).pergunta()).isEqualTo("Há quanto tempo?");
        assertThat(resposta.getPerguntasERespostas().get(0).resposta()).isEqualTo("Dois dias");
        assertThat(resposta.getPerguntasERespostas().get(1).resposta()).isNull();
    }

    @Test
    void coletaInconclusivaEPoucosDocumentosEntramNaNotaDeLimitacao() {
        prepararAnalise(MotivoEncerramento.LIMITE_PERGUNTAS,
                new ContextoRag("contexto", List.of("febre-001"), RagService.NOTA_POUCOS_DOCUMENTOS));

        ResultadoTriagemResponse resposta = service.analisar(10L, dono);

        assertThat(resposta.getNotaLimitacao())
                .contains(TriagemService.NOTA_COLETA_INCONCLUSIVA)
                .contains(RagService.NOTA_POUCOS_DOCUMENTOS);
    }

    @Test
    void encerramentoAntecipadoEntraNaNotaDeLimitacao() {
        prepararAnalise(MotivoEncerramento.ANTECIPADO, new ContextoRag("contexto", List.of(), RagService.NOTA_SEM_DOCUMENTOS));

        ResultadoTriagemResponse resposta = service.analisar(10L, dono);

        assertThat(resposta.getNotaLimitacao())
                .contains(TriagemService.NOTA_ENCERRAMENTO_ANTECIPADO)
                .contains(RagService.NOTA_SEM_DOCUMENTOS);
        assertThat(resultadoSalvo().getDocumentosRagUsados()).isEqualTo("[]");
    }

    @Test
    void analisarSessaoJaConcluidaDevolveOResultadoExistenteSemChamarAIa() {
        prepararAnalise(MotivoEncerramento.SUFICIENTE, new ContextoRag("contexto", List.of(), null));
        sessao.setStatus(StatusSessao.CONCLUIDA);
        sessao.setFinalizadaEm(LocalDateTime.now());
        when(resultadoRepository.findBySessaoId(10L)).thenReturn(Optional.of(ResultadoTriagem.builder()
                .sessao(sessao).nivelUrgencia(NivelUrgencia.LEVE).condicoesJson("[\"resfriado\"]")
                .explicacaoEducativa("e").orientacoesGerais("o").sinaisAlerta("[\"s\"]")
                .quandoBuscarAtendimento("q").disclaimer(ValidadorEtico.DISCLAIMER)
                .documentosRagUsados("[\"coriza-001\"]").build()));

        ResultadoTriagemResponse resposta = service.analisar(10L, dono);

        assertThat(resposta.getNivelUrgencia()).isEqualTo(NivelUrgencia.LEVE);
        assertThat(resposta.getCondicoes()).containsExactly("resfriado");
        assertThat(resposta.getFontes()).extracting(fonte -> fonte.sintoma()).containsExactly("coriza e nariz entupido");
        verify(ragService, never()).recuperar(any(), anyList());
        verify(analiseService, never()).analisar(any(), anyList(), any());
        verify(sessaoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusSessao.class, names = {"EM_PROGRESSO", "CANCELADA", "ERRO"})
    void analisarSessaoQueNaoEstaAnalisandoRetornaTransicaoInvalida(StatusSessao status) {
        sessao.setStatus(status);

        assertThatThrownBy(() -> service.analisar(10L, dono)).isInstanceOf(TransicaoInvalidaException.class);
        verify(analiseService, never()).analisar(any(), anyList(), any());
    }

    @Test
    void analisarSessaoDeOutroUsuarioRetornaNaoEncontrada() {
        sessao.setStatus(StatusSessao.ANALISANDO);

        assertThatThrownBy(() -> service.analisar(10L, outro)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(analiseService, never()).analisar(any(), anyList(), any());
    }

    @Test
    void falhaDaIaNaAnaliseMantemASessaoAnalisandoParaTentarDeNovo() {
        prepararAnalise(MotivoEncerramento.SUFICIENTE, new ContextoRag("contexto", List.of(), null));
        when(analiseService.analisar(any(), anyList(), any())).thenThrow(new IaIndisponivelException("fora"));

        assertThatThrownBy(() -> service.analisar(10L, dono)).isInstanceOf(IaIndisponivelException.class);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.ANALISANDO);
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void analiseConcorrenteQueJaGravouOResultadoDevolveOExistente() {
        prepararAnalise(MotivoEncerramento.SUFICIENTE, new ContextoRag("contexto", List.of(), null));
        when(sessaoRepository.save(any(SessaoTriagem.class))).thenThrow(new DataIntegrityViolationException("duplicado"));
        when(resultadoRepository.findBySessaoId(10L)).thenReturn(Optional.of(ResultadoTriagem.builder()
                .sessao(sessao).nivelUrgencia(NivelUrgencia.URGENTE).condicoesJson("[\"x\"]")
                .explicacaoEducativa("e").orientacoesGerais("o").sinaisAlerta("[]")
                .quandoBuscarAtendimento("q").disclaimer(ValidadorEtico.DISCLAIMER).build()));

        ResultadoTriagemResponse resposta = service.analisar(10L, dono);

        assertThat(resposta.getNivelUrgencia()).isEqualTo(NivelUrgencia.URGENTE);
        assertThat(resposta.getFontes()).isEmpty();
    }

    @Test
    void obterResultadoDeSessaoConcluida() {
        sessao.setStatus(StatusSessao.CONCLUIDA);
        when(mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(anyLong())).thenReturn(List.of());
        when(resultadoRepository.findBySessaoId(10L)).thenReturn(Optional.of(ResultadoTriagem.builder()
                .sessao(sessao).nivelUrgencia(NivelUrgencia.EMERGENCIA).condicoesJson("[\"infarto\"]")
                .explicacaoEducativa("e").orientacoesGerais("o").sinaisAlerta("[\"Dor no peito\"]")
                .quandoBuscarAtendimento("Ligue 192 agora.").disclaimer(ValidadorEtico.DISCLAIMER)
                .documentosRagUsados("[\"dor-no-peito-001\", \"id-removido-da-base\"]").build()));

        ResultadoTriagemResponse resposta = service.obterResultado(10L, dono);

        assertThat(resposta.getNivelUrgencia()).isEqualTo(NivelUrgencia.EMERGENCIA);
        assertThat(resposta.getFontes()).hasSize(1);
        assertThat(resposta.getPerguntasERespostas()).isEmpty();
    }

    @Test
    void obterResultadoDeSessaoNaoConcluidaRetornaTransicaoInvalida() {
        sessao.setStatus(StatusSessao.ANALISANDO);

        assertThatThrownBy(() -> service.obterResultado(10L, dono)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void obterResultadoDeOutroUsuarioRetornaNaoEncontrada() {
        sessao.setStatus(StatusSessao.CONCLUIDA);

        assertThatThrownBy(() -> service.obterResultado(10L, outro)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
