package com.orientasaude.service;

import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import com.orientasaude.dto.response.IniciarTriagemResponse;
import com.orientasaude.dto.response.MensagemTriagemResponse;
import com.orientasaude.exception.EntradaInvalidaException;
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
import com.orientasaude.util.Sanitizador;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a triagem: cria a sessão, grava perguntas e respostas e controla o status.
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

    private static final int MINIMO_CARACTERES_SINTOMAS = 10;
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");

    private final SessaoTriagemRepository sessaoRepository;
    private final MensagemTriagemRepository mensagemRepository;
    private final EntrevistaService entrevistaService;

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
     * Encerramento antecipado pedido pelo usuário (Requisito 4.6).
     */
    @Transactional
    public MensagemTriagemResponse encerrar(Long sessaoId, Usuario usuario) {
        SessaoTriagem sessao = buscarSessaoEmProgresso(sessaoId, usuario);
        finalizarEntrevista(sessao, MotivoEncerramento.ANTECIPADO);
        return new MensagemTriagemResponse(null, true, AVISO_ENCERRAMENTO_ANTECIPADO);
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
