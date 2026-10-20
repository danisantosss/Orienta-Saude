package com.orientasaude.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.GeminiConfig;
import com.orientasaude.exception.IaIndisponivelException;
import com.orientasaude.ia.AnaliseGerada;
import com.orientasaude.ia.GeminiClient;
import com.orientasaude.ia.PromptBuilder;
import com.orientasaude.ia.ValidadorEtico;
import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.rag.ContextoRag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gera a análise final com o Gemini (Requisitos 6.1, 6.2 e 6.7).
 * Faz a primeira chamada e até 2 novas tentativas, com intervalo entre elas e um limite total
 * de tempo. Uma tentativa falha quando o Gemini cai, quando o JSON vem inválido ou incompleto,
 * ou quando o ValidadorEtico encontra diagnóstico ou prescrição; nesse último caso a próxima
 * tentativa usa o prompt de reforço. Se nenhuma der certo, lança IaIndisponivelException (503).
 */
@Service
public class AnaliseService {

    private static final int TENTATIVAS = 3;
    private static final int MAXIMO_CONDICOES = 5;
    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

    private final GeminiClient geminiClient;
    private final PromptBuilder promptBuilder;
    private final ValidadorEtico validadorEtico;
    private final ObjectMapper objectMapper;
    private final long intervaloTentativasMs;
    private final long limiteTotalMs;

    @Autowired
    public AnaliseService(GeminiClient geminiClient, PromptBuilder promptBuilder, ValidadorEtico validadorEtico,
                          ObjectMapper objectMapper, GeminiConfig config) {
        this(geminiClient, promptBuilder, validadorEtico, objectMapper,
                config.getIntervaloTentativasMs(), config.getLimiteTotalAnaliseMs());
    }

    public AnaliseService(GeminiClient geminiClient, PromptBuilder promptBuilder, ValidadorEtico validadorEtico,
                          ObjectMapper objectMapper, long intervaloTentativasMs, long limiteTotalMs) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
        this.validadorEtico = validadorEtico;
        this.objectMapper = objectMapper;
        this.intervaloTentativasMs = intervaloTentativasMs;
        this.limiteTotalMs = limiteTotalMs;
    }

    public AnaliseGerada analisar(SessaoTriagem sessao, List<MensagemTriagem> mensagens, ContextoRag contexto) {
        String promptBase = promptBuilder.montarPromptAnalise(sessao, mensagens, contexto.texto());
        String prompt = promptBase;
        long prazo = System.currentTimeMillis() + limiteTotalMs;

        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            if (tentativa > 1 && !esperarProximaTentativa(prazo)) {
                break;
            }
            try {
                AnaliseGerada analise = interpretar(chamarDentroDoPrazo(prompt, prazo));
                List<String> termosProibidos = validadorEtico.validar(analise);
                if (termosProibidos.isEmpty()) {
                    return analise;
                }
                log.warn("Tentativa {} da análise recusada pela validação ética: {}", tentativa, termosProibidos);
                prompt = promptBuilder.montarPromptReforco(promptBase, termosProibidos);
            } catch (IaIndisponivelException | RespostaInvalidaException e) {
                log.warn("Tentativa {} da análise falhou: {}", tentativa, e.getMessage());
            }
        }
        throw new IaIndisponivelException("Não foi possível gerar a análise da triagem");
    }

    /**
     * Cada chamada só pode usar o tempo que sobrou do limite total.
     */
    private String chamarDentroDoPrazo(String prompt, long prazo) {
        long restante = prazo - System.currentTimeMillis();
        if (restante <= 0) {
            throw new IaIndisponivelException("Limite de tempo da análise esgotado");
        }
        CompletableFuture<String> chamada = CompletableFuture.supplyAsync(() -> geminiClient.gerarJson(prompt));
        try {
            return chamada.get(restante, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            chamada.cancel(true);
            throw new IaIndisponivelException("Limite de tempo da análise esgotado");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IaIndisponivelException falha) {
                throw falha;
            }
            throw new IaIndisponivelException("Falha ao chamar a API Gemini", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaIndisponivelException("Análise interrompida");
        }
    }

    private boolean esperarProximaTentativa(long prazo) {
        if (System.currentTimeMillis() + intervaloTentativasMs >= prazo) {
            return false;
        }
        try {
            Thread.sleep(intervaloTentativasMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private AnaliseGerada interpretar(String resposta) {
        JsonNode json;
        try {
            json = objectMapper.readTree(removerBlocoDeCodigo(resposta));
        } catch (JsonProcessingException e) {
            throw new RespostaInvalidaException("a resposta não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw new RespostaInvalidaException("a resposta não é um objeto JSON");
        }

        List<String> condicoes = lerLista(json, "condicoes");
        if (condicoes.isEmpty()) {
            throw new RespostaInvalidaException("nenhuma condição informada");
        }
        if (condicoes.size() > MAXIMO_CONDICOES) {
            condicoes = condicoes.subList(0, MAXIMO_CONDICOES);
        }

        return new AnaliseGerada(
                validadorEtico.interpretarNivel(json.path("nivelUrgencia").asText(null)),
                List.copyOf(condicoes),
                lerTexto(json, "explicacaoEducativa"),
                lerTexto(json, "orientacoesGerais"),
                lerLista(json, "sinaisAlerta"),
                lerTexto(json, "quandoBuscarAtendimento"));
    }

    /**
     * Alguns modelos devolvem o JSON dentro de ```json ... ```, mesmo pedindo JSON puro.
     */
    private String removerBlocoDeCodigo(String resposta) {
        String texto = resposta.trim();
        if (texto.startsWith("```")) {
            texto = texto.replaceFirst("^```[a-zA-Z]*", "");
            int fim = texto.lastIndexOf("```");
            if (fim >= 0) {
                texto = texto.substring(0, fim);
            }
        }
        return texto.trim();
    }

    private String lerTexto(JsonNode json, String campo) {
        String valor = json.path(campo).asText("").trim();
        if (valor.isEmpty()) {
            throw new RespostaInvalidaException("campo obrigatório vazio: " + campo);
        }
        return valor;
    }

    private List<String> lerLista(JsonNode json, String campo) {
        List<String> valores = new ArrayList<>();
        for (JsonNode item : json.path(campo)) {
            String valor = item.asText("").trim();
            if (!valor.isEmpty()) {
                valores.add(valor);
            }
        }
        return valores;
    }

    /** Resposta do Gemini que chegou, mas não serve (JSON inválido ou incompleto). */
    private static class RespostaInvalidaException extends RuntimeException {
        RespostaInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
