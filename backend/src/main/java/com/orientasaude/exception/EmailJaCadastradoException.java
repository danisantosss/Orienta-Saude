package com.orientasaude.exception;

/**
 * Exceção lançada quando um email já está cadastrado no sistema.
 */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("O email '" + email + "' já está em uso");
    }
}
