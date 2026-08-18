package com.orientasaude.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
