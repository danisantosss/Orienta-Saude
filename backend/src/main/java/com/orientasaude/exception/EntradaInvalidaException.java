package com.orientasaude.exception;

/**
 * Exceção lançada quando o texto do usuário fica inválido depois da sanitização
 * (ex.: sobra menos de 10 caracteres de sintomas). Vira HTTP 400.
 */
public class EntradaInvalidaException extends RuntimeException {

    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
