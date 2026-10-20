package com.orientasaude.rag;

import com.orientasaude.model.MensagemTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.PapelMensagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Monta a consulta da triagem (sintomas + respostas), recupera os documentos e transforma
 * o resultado no contexto do prompt de análise (Requisitos 5.1, 5.2, 5.4 e 5.8).
 */
@Service
public class RagService {

    public static final String NOTA_POUCOS_DOCUMENTOS =
            "Poucas fontes da base de conhecimento combinaram com os sintomas informados, então as orientações são mais gerais.";
    public static final String NOTA_SEM_DOCUMENTOS =
            "Nenhuma fonte da base de conhecimento combinou com os sintomas informados. As orientações foram geradas só pela IA e devem ser vistas com mais cautela.";
    public static final String CONTEXTO_VAZIO = "Nenhum documento relevante encontrado.";

    private static final int MINIMO_DOCUMENTOS = 3;
    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final BuscadorDocumentos buscador;

    public RagService(BuscadorDocumentos buscador) {
        this.buscador = buscador;
    }

    public ContextoRag recuperar(SessaoTriagem sessao, List<MensagemTriagem> mensagens) {
        ResultadoBusca resultado = buscador.buscar(montarConsulta(sessao, mensagens));
        List<DocumentoConhecimento> documentos = resultado.documentos().stream()
                .map(DocumentoEncontrado::documento)
                .toList();
        List<String> ids = documentos.stream().map(DocumentoConhecimento::id).toList();

        if (documentos.isEmpty()) {
            log.warn("Nenhum documento recuperado para a sessão {} (método {})", sessao.getId(), resultado.metodo());
            return new ContextoRag(CONTEXTO_VAZIO, ids, NOTA_SEM_DOCUMENTOS);
        }
        String nota = documentos.size() < MINIMO_DOCUMENTOS ? NOTA_POUCOS_DOCUMENTOS : null;
        return new ContextoRag(montarContexto(documentos), ids, nota);
    }

    /**
     * Sintomas iniciais e respostas do usuário; as perguntas ficam de fora porque só repetiriam termos.
     */
    private String montarConsulta(SessaoTriagem sessao, List<MensagemTriagem> mensagens) {
        StringBuilder consulta = new StringBuilder(sessao.getSintomasIniciais());
        for (MensagemTriagem mensagem : mensagens) {
            if (mensagem.getPapel() == PapelMensagem.USUARIO) {
                consulta.append("\n").append(mensagem.getConteudo());
            }
        }
        return consulta.toString();
    }

    private String montarContexto(List<DocumentoConhecimento> documentos) {
        StringBuilder contexto = new StringBuilder();
        for (DocumentoConhecimento documento : documentos) {
            Map<String, String> indicadores = documento.indicadoresUrgencia();
            contexto.append("[").append(documento.id()).append("] ")
                    .append("Fonte: ").append(documento.fonte()).append(" (").append(documento.urlFonte()).append(")\n")
                    .append("Sintoma: ").append(documento.sintoma()).append("\n")
                    .append(documento.conteudo()).append("\n")
                    .append("Indicadores de urgência: leve: ").append(indicadores.get("leve"))
                    .append("; moderado: ").append(indicadores.get("moderado"))
                    .append("; urgente: ").append(indicadores.get("urgente"))
                    .append("; emergência: ").append(indicadores.get("emergencia")).append("\n")
                    .append("Orientações gerais: ").append(documento.orientacoesGerais()).append("\n")
                    .append("Sinais de alerta: ").append(String.join(", ", documento.sinaisAlerta())).append("\n\n");
        }
        return contexto.toString().trim();
    }
}
