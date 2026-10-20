package com.orientasaude.exception;

/**
 * Exceção lançada quando a sessão de triagem não está no status que a operação exige
 * (ex.: enviar mensagem numa sessão já encerrada). Vira HTTP 422.
 */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
