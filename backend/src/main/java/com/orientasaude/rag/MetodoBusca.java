package com.orientasaude.rag;

/**
 * Como os documentos foram encontrados: por embeddings (busca semântica), pela alternativa
 * de palavras-chave ou sem busca (erro inesperado).
 */
public enum MetodoBusca {
    EMBEDDINGS,
    PALAVRAS_CHAVE,
    INDISPONIVEL
}
