package com.orientasaude.rag;

import java.util.List;
import java.util.Map;

/**
 * Um documento da base de conhecimento, lido de um arquivo JSON em knowledge-base/.
 */
public record DocumentoConhecimento(
        String id,
        String sintoma,
        List<String> palavrasChave,
        String fonte,
        String urlFonte,
        String conteudo,
        List<String> condicoesRelacionadas,
        Map<String, String> indicadoresUrgencia,
        String orientacoesGerais,
        List<String> sinaisAlerta) {

    /**
     * Texto que vira embedding: sintoma, conteúdo e palavras-chave juntos.
     */
    public String textoIndexavel() {
        return sintoma + "\n" + conteudo + "\nPalavras-chave: " + String.join(", ", palavrasChave);
    }
}
