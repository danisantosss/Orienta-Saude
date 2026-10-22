package com.orientasaude.ia;

import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.PapelMensagem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monta os prompts enviados ao Gemini.
 * Usa somente idade, sintomas e o histórico de perguntas e respostas:
 * nunca nome, email ou id do usuário.
 */
@Component
public class PromptBuilder {

    public static final String MARCADOR_ENTREVISTA_COMPLETA = "[ENTREVISTA_COMPLETA]";
    public static final int MINIMO_PERGUNTAS = 2;
    public static final int MAXIMO_PERGUNTAS = 10;

    public String montarPromptEntrevista(SessaoTriagem sessao, List<MensagemTriagem> mensagens) {
        long perguntasFeitas = mensagens.stream()
                .filter(MensagemTriagem::isPerguntaDaEntrevista)
                .count();

        StringBuilder prompt = new StringBuilder()
                .append("Você é um assistente de triagem de saúde educativo.\n")
                .append("Sua função é fazer perguntas complementares para entender melhor os sintomas.\n")
                .append("NÃO faça diagnóstico. NÃO prescreva medicamentos.\n\n")
                .append("Paciente: ").append(sessao.getIdadePaciente()).append(" anos\n")
                .append("Sintomas iniciais: ").append(sessao.getSintomasIniciais()).append("\n")
                .append("Histórico da conversa:").append(montarHistorico(mensagens)).append("\n")
                .append("Perguntas já feitas: ").append(perguntasFeitas)
                .append(" (mínimo ").append(MINIMO_PERGUNTAS)
                .append(", máximo ").append(MAXIMO_PERGUNTAS).append(")\n\n")
                .append("Regras:\n")
                .append("- Faça UMA pergunta por vez, clara e objetiva, em linguagem simples\n")
                .append("- Baseie a pergunta nas respostas anteriores\n");

        if (perguntasFeitas < MINIMO_PERGUNTAS) {
            prompt.append("- Ainda NÃO encerre a entrevista: faça obrigatoriamente uma pergunta\n\n")
                    .append("Responda APENAS com a próxima pergunta.");
        } else {
            prompt.append("- Quando tiver informações suficientes, responda apenas: ")
                    .append(MARCADOR_ENTREVISTA_COMPLETA).append("\n\n")
                    .append("Responda APENAS com a próxima pergunta ou ")
                    .append(MARCADOR_ENTREVISTA_COMPLETA).append(".");
        }
        return prompt.toString();
    }

    /**
     * Prompt da análise final: entrevista completa + contexto recuperado pelo RAG, com resposta em JSON.
     */
    public String montarPromptAnalise(SessaoTriagem sessao, List<MensagemTriagem> mensagens, String contextoRag) {
        return new StringBuilder()
                .append("Você é um assistente educativo de triagem de saúde.\n")
                .append("Analise as informações abaixo e oriente a pessoa sobre o nível de urgência.\n\n")
                .append("Paciente: ").append(sessao.getIdadePaciente()).append(" anos\n")
                .append("Sintomas iniciais: ").append(sessao.getSintomasIniciais()).append("\n")
                .append("Informações coletadas na entrevista:").append(montarHistorico(mensagens)).append("\n\n")
                .append("Contexto de fontes confiáveis:\n").append(contextoRag).append("\n\n")
                .append("Níveis de urgência:\n")
                .append("- EMERGENCIA: risco imediato; procurar o pronto-socorro agora ou ligar para o SAMU (192)\n")
                .append("- URGENTE: procurar atendimento médico em até 24 horas\n")
                .append("- MODERADO: agendar consulta em até 7 dias\n")
                .append("- LEVE: cuidados em casa e observação dos sintomas\n\n")
                .append("Responda OBRIGATORIAMENTE em JSON, neste formato:\n")
                .append("{\n")
                .append("  \"nivelUrgencia\": \"LEVE|MODERADO|URGENTE|EMERGENCIA\",\n")
                .append("  \"condicoes\": [\"condição 1\", \"condição 2\"],\n")
                .append("  \"explicacaoEducativa\": \"...\",\n")
                .append("  \"orientacoesGerais\": \"...\",\n")
                .append("  \"sinaisAlerta\": [\"sinal 1\", \"sinal 2\"],\n")
                .append("  \"quandoBuscarAtendimento\": \"...\"\n")
                .append("}\n\n")
                .append("Regras OBRIGATÓRIAS:\n")
                .append("- NÃO afirme diagnóstico (use \"pode estar relacionado a\")\n")
                .append("- NÃO prescreva medicamentos, dosagens ou tratamentos\n")
                .append("- NÃO use termos como \"você tem\" ou \"diagnóstico confirmado\"\n")
                .append("- Entre 1 e 5 condições possivelmente relacionadas\n")
                .append("- Na dúvida entre dois níveis, escolha o mais grave\n")
                .append("- Use linguagem simples e baseie-se no contexto das fontes quando ele existir\n")
                .append("- Inclua sempre a recomendação de procurar avaliação de um profissional de saúde")
                .toString();
    }

    /**
     * Prompt de reforço: o mesmo prompt de análise com os trechos proibidos encontrados na resposta anterior.
     */
    public String montarPromptReforco(String promptAnalise, List<String> termosProibidos) {
        return promptAnalise
                + "\n\nATENÇÃO: a resposta anterior foi recusada porque continha os trechos proibidos: "
                + String.join(", ", termosProibidos)
                + ".\nReescreva a resposta inteira sem esses trechos, sem afirmar diagnóstico e sem citar medicamentos ou doses.";
    }

    private String montarHistorico(List<MensagemTriagem> mensagens) {
        StringBuilder historico = new StringBuilder();
        for (MensagemTriagem mensagem : mensagens) {
            if (mensagem.isPerguntaDaEntrevista()) {
                historico.append("\nPergunta: ").append(mensagem.getConteudo());
            } else if (mensagem.getPapel() == PapelMensagem.USUARIO) {
                historico.append("\nResposta: ").append(mensagem.getConteudo());
            }
        }
        return historico.isEmpty() ? " (nenhuma pergunta feita ainda)" : historico.toString();
    }
}
