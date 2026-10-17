package com.orientasaude.ia;

import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.PapelMensagem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monta os prompts enviados ao Gemini.
 * Usa somente idade, sintomas e o histórico de perguntas e respostas:
 * nunca nome, email ou id do usuário (Requisito 15.3).
 */
@Component
public class PromptBuilder {

    public static final String MARCADOR_ENTREVISTA_COMPLETA = "[ENTREVISTA_COMPLETA]";
    public static final int MINIMO_PERGUNTAS = 2;
    public static final int MAXIMO_PERGUNTAS = 10;

    public String montarPromptEntrevista(SessaoTriagem sessao, List<MensagemTriagem> mensagens) {
        long perguntasFeitas = mensagens.stream()
                .filter(m -> m.getPapel() == PapelMensagem.ASSISTENTE)
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

    private String montarHistorico(List<MensagemTriagem> mensagens) {
        StringBuilder historico = new StringBuilder();
        for (MensagemTriagem mensagem : mensagens) {
            if (mensagem.getPapel() == PapelMensagem.ASSISTENTE) {
                historico.append("\nPergunta: ").append(mensagem.getConteudo());
            } else if (mensagem.getPapel() == PapelMensagem.USUARIO) {
                historico.append("\nResposta: ").append(mensagem.getConteudo());
            }
        }
        return historico.isEmpty() ? " (nenhuma pergunta feita ainda)" : historico.toString();
    }
}
