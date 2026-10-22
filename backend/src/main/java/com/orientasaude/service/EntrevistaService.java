package com.orientasaude.service;

import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.GeminiClient;
import com.orientasaude.ia.PromptBuilder;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.repository.MensagemTriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.orientasaude.ia.PromptBuilder.MARCADOR_ENTREVISTA_COMPLETA;
import static com.orientasaude.ia.PromptBuilder.MAXIMO_PERGUNTAS;
import static com.orientasaude.ia.PromptBuilder.MINIMO_PERGUNTAS;

/**
 * Conduz a entrevista adaptativa: pede ao Gemini a próxima pergunta
 * e aplica os limites de no mínimo 2 e no máximo 10 perguntas.
 * Não persiste nada; quem grava as mensagens é o TriagemService.
 */
@Service
@RequiredArgsConstructor
public class EntrevistaService {

    private final GeminiClient geminiClient;
    private final PromptBuilder promptBuilder;
    private final MensagemTriagemRepository mensagemRepository;

    public ResultadoEntrevista gerarProximaPergunta(SessaoTriagem sessao) {
        List<MensagemTriagem> mensagens = mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(sessao.getId());
        long perguntasFeitas = contar(mensagens);

        if (perguntasFeitas >= MAXIMO_PERGUNTAS) {
            return ResultadoEntrevista.inconclusivaPorLimite();
        }

        String prompt = promptBuilder.montarPromptEntrevista(sessao, mensagens);
        String resposta = geminiClient.gerarTexto(prompt);

        if (resposta.contains(MARCADOR_ENTREVISTA_COMPLETA)) {
            if (perguntasFeitas >= MINIMO_PERGUNTAS) {
                return ResultadoEntrevista.concluida();
            }
            // Abaixo do mínimo o Gemini não pode encerrar: pede de novo uma única vez
            resposta = geminiClient.gerarTexto(prompt);
            if (resposta.contains(MARCADOR_ENTREVISTA_COMPLETA)) {
                throw new IaIndisponivelException("O Gemini encerrou a entrevista antes do mínimo de perguntas");
            }
        }

        String pergunta = resposta.trim();
        if (pergunta.isEmpty()) {
            throw new IaIndisponivelException("O Gemini devolveu uma pergunta vazia");
        }
        return ResultadoEntrevista.novaPergunta(pergunta);
    }

    public long contarPerguntasFeitas(SessaoTriagem sessao) {
        return contar(mensagemRepository.findBySessaoIdOrderByOrdemSequenciaAsc(sessao.getId()));
    }

    private long contar(List<MensagemTriagem> mensagens) {
        return mensagens.stream()
                .filter(MensagemTriagem::isPerguntaDaEntrevista)
                .count();
    }
}
