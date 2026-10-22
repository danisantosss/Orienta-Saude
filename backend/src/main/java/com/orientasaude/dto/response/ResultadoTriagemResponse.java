package com.orientasaude.dto.response;

import com.orientasaude.model.enums.NivelUrgencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado completo da triagem: classificação, orientações, disclaimer, fontes consultadas
 * e as perguntas e respostas da entrevista.
 */
@Data
@Builder
@AllArgsConstructor
public class ResultadoTriagemResponse {
    private Long sessaoId;
    private String sintomasIniciais;
    private Integer idadePaciente;
    private NivelUrgencia nivelUrgencia;
    private List<String> condicoes;
    private String explicacaoEducativa;
    private String orientacoesGerais;
    private List<String> sinaisAlerta;
    private String quandoBuscarAtendimento;
    private String notaLimitacao;
    private String disclaimer;
    private List<Fonte> fontes;
    private List<PerguntaResposta> perguntasERespostas;
    private LocalDateTime concluidaEm;

    /** Documento da base de conhecimento usado na análise. */
    public record Fonte(String sintoma, String fonte, String url) {
    }

    /** Uma pergunta da entrevista e a resposta do usuário (nula se a entrevista foi encerrada antes). */
    public record PerguntaResposta(String pergunta, String resposta) {
    }
}
