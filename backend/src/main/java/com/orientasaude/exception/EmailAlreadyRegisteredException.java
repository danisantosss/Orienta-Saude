package com.orientasaude.exception;

/**
 * Thrown when an email is already registered in the system.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("O email '" + email + "' já está em uso");
    }
}
