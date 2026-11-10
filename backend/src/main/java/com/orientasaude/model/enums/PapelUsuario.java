package com.orientasaude.model.enums;

/**
 * Papel da conta.
 * USUARIO: quem faz triagens (toda conta criada pelo cadastro público)
 * ADMIN: vê só os indicadores agregados do painel; atribuído no banco ou por variável de ambiente
 */
public enum PapelUsuario {
    USUARIO,
    ADMIN
}
