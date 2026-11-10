package com.orientasaude.exception;

import lombok.Getter;

/**
 * Login recusado porque o IP errou a senha muitas vezes seguidas (HTTP 429).
 */
@Getter
public class LoginBloqueadoException extends RuntimeException {

    private final long segundosRestantes;

    public LoginBloqueadoException(long segundosRestantes) {
        super("Muitas tentativas de login sem sucesso. Tente novamente em "
                + minutosParaExibir(segundosRestantes) + ".");
        this.segundosRestantes = segundosRestantes;
    }

    private static String minutosParaExibir(long segundos) {
        long minutos = Math.max(1, (segundos + 59) / 60);
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
