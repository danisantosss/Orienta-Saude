package com.orientasaude.model.enums;

/**
 * Eventos de segurança gravados no log de auditoria. Nenhum deles carrega dado de saúde.
 */
public enum TipoEventoAuditoria {
    /** Email ou senha errados no login */
    LOGIN_FALHO,
    /** O IP chegou ao limite de falhas seguidas e o login ficou bloqueado */
    LOGIN_BLOQUEADO,
    /** Tentativa de abrir a triagem de outro usuário */
    ACESSO_NEGADO,
    /** Usuário sem papel ADMIN tentou acessar a área administrativa */
    PERMISSAO_NEGADA,
    /** Token ausente da base, adulterado ou expirado */
    TOKEN_INVALIDO,
    /** O IP passou do limite de requisições por minuto */
    LIMITE_EXCEDIDO,
    /** O titular pediu a exclusão da conta e dos dados */
    EXCLUSAO_SOLICITADA
}
