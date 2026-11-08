package com.orientasaude.exception;

/**
 * Senha de confirmação errada numa ação sensível (ex.: excluir a conta). Vira HTTP 403.
 * Não usa 401 porque o usuário continua logado; só a confirmação falhou.
 */
public class SenhaIncorretaException extends RuntimeException {

    public SenhaIncorretaException() {
        super("Senha incorreta");
    }
}
