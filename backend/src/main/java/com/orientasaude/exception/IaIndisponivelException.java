package com.orientasaude.exception;

/**
 * Exceção lançada quando a API Gemini falha, excede o tempo limite
 * ou devolve uma resposta que não pode ser usada.
 */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
