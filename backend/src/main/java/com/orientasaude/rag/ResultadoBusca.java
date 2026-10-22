package com.orientasaude.rag;

import java.util.List;

/**
 * Documentos recuperados, do mais para o menos relevante, e o método usado na busca.
 */
public record ResultadoBusca(List<DocumentoEncontrado> documentos, MetodoBusca metodo) {
}
