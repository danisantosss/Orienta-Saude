package com.orientasaude.rag;

/**
 * Documento recuperado e o quanto ele combina com a consulta (0 a 1).
 */
public record DocumentoEncontrado(DocumentoConhecimento documento, double relevancia) {
}
