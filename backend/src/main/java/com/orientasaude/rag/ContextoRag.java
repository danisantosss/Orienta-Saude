package com.orientasaude.rag;

import java.util.List;

/**
 * O que o RAG entrega para a análise: o texto que vai no prompt, os ids dos documentos usados
 * (gravados no resultado) e o aviso de limitação, quando houver poucos ou nenhum documento.
 */
public record ContextoRag(String texto, List<String> idsDocumentos, String notaLimitacao) {
}
