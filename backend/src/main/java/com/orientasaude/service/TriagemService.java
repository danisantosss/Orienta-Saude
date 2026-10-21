package com.orientasaude.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import com.orientasaude.dto.response.IniciarTriagemResponse;
import com.orientasaude.dto.response.MensagemTriagemResponse;
import com.orientasaude.dto.response.ResultadoTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
import com.orientasaude.exception.RecursoNaoEncontradoException;
import com.orientasaude.exception.TransicaoInvalidaException;
import com.orientasaude.ia.AnaliseGerada;
import com.orientasaude.ia.ValidadorEtico;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.MotivoEncerramento;
import com.orientasaude.model.enums.PapelMensagem;
import com.orientasaude.model.enums.StatusSessao;
import com.orientasaude.rag.BaseConhecimento;
import com.orientasaude.rag.ContextoRag;
import com.orientasaude.rag.RagService;
import com.orientasaude.repository.MensagemTriagemRepository;
import com.orientasaude.repository.ResultadoTriagemRepository;
import com.orientasaude.repository.SessaoTriagemRepository;
import com.orientasaude.util.Sanitizador;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orquestra a triagem: cria a sessão, grava perguntas e respostas, gera a análise final
 * e controla o status.
 * Cada operação é uma transação: se o Gemini falhar no meio, nada do que foi gravado
 * naquela chamada fica no banco, e o usuário pode repetir sem perder o contexto anterior.
 */
@Service
@RequiredArgsConstructor
public class TriagemService {

    public static final String AVISO_COLETA_INCONCLUSIVA =
            "A entrevista atingiu o limite de 10 perguntas. O resultado vai indicar que a coleta de informações foi inconclusiva.";
    public static final String AVISO_ENCERRAMENTO_ANTECIPADO =
            "Entrevista encerrada antes da conclusão. O resultado será gerado com os dados disponíveis e pode ser menos preciso.";
    public static final String NOTA_COLETA_INCONCLUSIVA =
            "A coleta de informações foi inconclusiva: a entrevista chegou ao limite de 10 perguntas.";
    public static final String NOTA_ENCERRAMENTO_ANTECIPADO =
            "A entrevista foi encerrada antes da conclusão, então o resultado pode ser menos preciso.";

    private static final int MINIMO_CARACTERES_SINTOMAS = 10;
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    private final SessaoTriagemRepository sessaoRepository;
    private final MensagemTriagemRepository mensagemRepository;
    private final ResultadoTriagemRepository resultadoRepository;
    private final EntrevistaService entrevistaService;
    private final RagService ragService;
    private final AnaliseService analiseService;
    private final BaseConhecimento baseConhecimento;
    private final ObjectMapper objectMapper;

    /**
     * Cria a sessão com os sintomas sanitizados e devolve a primeira pergunta do Gemini.
     */
    @Transactional
    public IniciarTriagemResponse iniciar(IniciarTriagemRequest request, Usuario usuario) {
        String sintomas = Sanitizador.sanitizar(request.getSintomas());
        if (sintomas.length() < MINIMO_CARACTERES_SINTOMAS) {
            throw new EntradaInvalidaException("Descreva os sintomas com mais detalhes (mínimo de 10 caracteres)");
        }

        SessaoTriagem sessao = sessaoRepository.save(SessaoTriagem.builder()
                .usuario(usuario)
                .sintomasIniciais(sintomas)
                .idadePaciente(request.getIdade())
                .status(StatusSessao.EM_PROGRESSO)
                .build());
        salvarMensagem(sessao, PapelMensagem.SISTEMA, sintomas, 1);

        ResultadoEntrevista resultado = entrevistaService.gerarProximaPergunta(sessao);
        salvarMensagem(sessao, PapelMensagem.ASSISTENTE, resultado.pergunta(), 2);

        return new IniciarTriagemResponse(sessao.getId(), resultado.pergunta());
    }

    /**
     * Grava a resposta do usuário e pede a próxima pergunta.
     * Quando a entrevista termina, a sessão passa para ANALISANDO.
     */
    @Transactional
    public MensagemTriagemResponse enviarMensagem(Long sessaoId, EnviarMensagemRequest request, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoEmProgresso(sessaoId, usuario);

        String conteudo = Sanitizador.sanitizar(request.getConteudo());
        if (conteudo.isEmpty()) {
            throw new EntradaInvalidaException("A resposta não pode ficar vazia");
        }

        int ordem = (int) mensagemRepository.countBySessaoId(sessaoId);
        salvarMensagem(sessao, PapelMensagem.USUARIO, conteudo, ordem + 1);

        ResultadoEntrevista resultado = entrevistaService.gerarProximaPergunta(sessao);
        if (resultado.completa()) {
            if (resultado.inconclusiva()) {
                finalizarEntrevista(sessao, MotivoEncerramento.LIMITE_PERGUNTAS);
                return new MensagemTriagemResponse(null, true, AVISO_COLETA_INCONCLUSIVA);
            }
            finalizarEntrevista(sessao, MotivoEncerramento.SUFICIENTE);
            return new MensagemTriagemResponse(null, true, null);
        }

        salvarMensagem(sessao, PapelMensagem.ASSISTENTE, resultado.pergunta(), ordem + 2);
        return new MensagemTriagemResponse(resultado.pergunta(), false, null);
    }

    /**
     * Encerramento antecipado pedido pelo usuário.
     */
    @Transactional
    public MensagemTriagemResponse encerrar(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoEmProgresso(sessaoId, usuario);
        finalizarEntrevista(sessao, MotivoEncerramento.ANTECIPADO);
        return new MensagemTriagemResponse(null, true, AVISO_ENCERRAMENTO_ANTECIPADO);
    }

    /**
     * Gera o resultado da triagem: RAG + análise do Gemini + validação ética, grava o resultado
     * com o disclaimer e conclui a sessão.
     * Idempotente: uma sessão já concluída devolve o resultado gravado, sem chamar a IA.
     * Não é uma transação única de propósito: a análise pode levar até 30 s e não deve segurar
     * uma conexão do banco. Se a IA falhar, nada é gravado e a sessão continua ANALISANDO.
     */
    public ResultadoTriagemResponse analisar(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoDoUsuario(sessaoId, usuario);
        if (sessao.getStatus() == StatusSessao.CONCLUIDA) {
            return obterResultadoGravado(sessao);
        }
        if (sessao.getStatus() != StatusSessao.ANALISANDO) {
            throw new TransicaoInvalidaException("A sessão só pode ser analisada depois que a entrevista terminar");
        }

        List<MensagemTriagem> mensagens = mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(sessaoId);
        ContextoRag contexto = ragService.recuperar(sessao, mensagens);
        AnaliseGerada analise = analiseService.analisar(sessao, mensagens, contexto);

        ResultadoTriagem resultado = ResultadoTriagem.builder()
                .sessao(sessao)
                .nivelUrgencia(analise.nivelUrgencia())
                .condicoesJson(paraJson(analise.condicoes()))
                .explicacaoEducativa(analise.explicacaoEducativa())
                .orientacoesGerais(analise.orientacoesGerais())
                .sinaisAlerta(paraJson(analise.sinaisAlerta()))
                .quandoBuscarAtendimento(analise.quandoBuscarAtendimento())
                .disclaimer(ValidadorEtico.DISCLAIMER)
                .notaLimitacao(montarNotaLimitacao(sessao, contexto))
                .documentosRagUsados(paraJson(contexto.idsDocumentos()))
                .build();
        sessao.setResultado(resultado);
        sessao.setStatus(StatusSessao.CONCLUIDA);
        sessao.setFinalizadaEm(LocalDateTime.now());
        try {
            sessaoRepository.save(sessao);
        } catch (DataIntegrityViolationException e) {
            // Outra requisição concluiu a mesma sessão ao mesmo tempo: vale o resultado que já foi gravado
            return obterResultadoGravado(sessao);
        }
        return montarResposta(sessao, resultado, mensagens);
    }

    /**
     * Resultado de uma sessão já concluída (GET /api/triagem/{id}/resultado).
     */
    public ResultadoTriagemResponse obterResultado(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoDoUsuario(sessaoId, usuario);
        if (sessao.getStatus() != StatusSessao.CONCLUIDA) {
            throw new TransicaoInvalidaException("O resultado desta sessão ainda não foi gerado");
        }
        return obterResultadoGravado(sessao);
    }

    private ResultadoTriagemResponse obterResultadoGravado(SessaoTriagem sessao) {
        ResultadoTriagem resultado = resultadoRepository.findBySessaoId(sessao.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Resultado da triagem não encontrado"));
        return montarResposta(sessao, resultado, mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(sessao.getId()));
    }

    /**
     * Junta os avisos de limitação: entrevista incompleta e poucas fontes encontradas pelo RAG.
     */
    private String montarNotaLimitacao(SessaoTriagem sessao, ContextoRag contexto) {
        List<String> notas = new ArrayList<>();
        if (sessao.getMotivoEncerramento() == MotivoEncerramento.LIMITE_PERGUNTAS) {
            notas.add(NOTA_COLETA_INCONCLUSIVA);
        } else if (sessao.getMotivoEncerramento() == MotivoEncerramento.ANTECIPADO) {
            notas.add(NOTA_ENCERRAMENTO_ANTECIPADO);
        }
        if (contexto.notaLimitacao() != null) {
            notas.add(contexto.notaLimitacao());
        }
        return notas.isEmpty() ? null : String.join(" ", notas);
    }

    private ResultadoTriagemResponse montarResposta(SessaoTriagem sessao, ResultadoTriagem resultado,
                                                    List<MensagemTriagem> mensagens) {
        List<ResultadoTriagemResponse.Fonte> fontes = deJson(resultado.getDocumentosRagUsados()).stream()
                .map(baseConhecimento::buscarPorId)
                .flatMap(Optional::stream)
                .map(documento -> new ResultadoTriagemResponse.Fonte(
                        documento.sintoma(), documento.fonte(), documento.urlFonte()))
                .toList();

        return ResultadoTriagemResponse.builder()
                .sessaoId(sessao.getId())
                .sintomasIniciais(sessao.getSintomasIniciais())
                .idadePaciente(sessao.getIdadePaciente())
                .nivelUrgencia(resultado.getNivelUrgencia())
                .condicoes(deJson(resultado.getCondicoesJson()))
                .explicacaoEducativa(resultado.getExplicacaoEducativa())
                .orientacoesGerais(resultado.getOrientacoesGerais())
                .sinaisAlerta(deJson(resultado.getSinaisAlerta()))
                .quandoBuscarAtendimento(resultado.getQuandoBuscarAtendimento())
                .notaLimitacao(resultado.getNotaLimitacao())
                .disclaimer(resultado.getDisclaimer())
                .fontes(fontes)
                .perguntasERespostas(montarPerguntasERespostas(mensagens))
                .concluidaEm(sessao.getFinalizadaEm())
                .build();
    }

    private List<ResultadoTriagemResponse.PerguntaResposta> montarPerguntasERespostas(List<MensagemTriagem> mensagens) {
        List<ResultadoTriagemResponse.PerguntaResposta> pares = new ArrayList<>();
        for (MensagemTriagem mensagem : mensagens) {
            if (mensagem.isPerguntaDaEntrevista()) {
                pares.add(new ResultadoTriagemResponse.PerguntaResposta(mensagem.getConteudo(), null));
            } else if (mensagem.getPapel() == PapelMensagem.USUARIO && !pares.isEmpty()) {
                int ultimo = pares.size() - 1;
                pares.set(ultimo, new ResultadoTriagemResponse.PerguntaResposta(pares.get(ultimo).pergunta(), mensagem.getConteudo()));
            }
        }
        return pares;
    }

    private String paraJson(List<String> valores) {
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível converter a lista para JSON", e);
        }
    }

    private List<String> deJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { }).stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resultado gravado com JSON inválido", e);
        }
    }

    /**
     * Só devolve a sessão se ela pertencer ao usuário. Sessão de outra pessoa responde
     * como inexistente (404), para não revelar que o id existe, e fica registrada na auditoria.
     */
    private SessaoTriagem buscarSessaoDoUsuario(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão de triagem não encontrada"));
        if (!sessao.getUsuario().getId().equals(usuario.getId())) {
            auditoria.warn("Acesso negado: usuário {} tentou acessar a sessão {} de outro usuário",
                    usuario.getId(), sessaoId);
            throw new RecursoNaoEncontradoException("Sessão de triagem não encontrada");
        }
        return sessao;
    }

    private SessaoTriagem buscarSessaoEmProgresso(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoDoUsuario(sessaoId, usuario);
        if (sessao.getStatus() != StatusSessao.EM_PROGRESSO) {
            throw new TransicaoInvalidaException("A entrevista desta sessão já foi encerrada");
        }
        return sessao;
    }

    private void finalizarEntrevista(SessaoTriagem sessao, MotivoEncerramento motivo) {
        sessao.setStatus(StatusSessao.ANALISANDO);
        sessao.setMotivoEncerramento(motivo);
        sessaoRepository.save(sessao);
    }

    private void salvarMensagem(SessaoTriagem sessao, PapelMensagem papel, String conteudo, int ordem) {
        mensagemRepository.save(MensagemTriagem.builder()
                .sessao(sessao)
                .papel(papel)
                .conteudo(conteudo)
                .ordemSequencia(ordem)
                .build());
    }
}
