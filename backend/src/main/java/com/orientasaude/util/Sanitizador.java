package com.orientasaude.util;

import java.util.regex.Pattern;

/**
 * Sanitiza texto livre informado pelo usuário antes de persistir.
 * Remove tags HTML e sequências comumente usadas em tentativas de SQL injection,
 * sem rejeitar a submissão (apenas limpa o conteúdo).
 */
public final class Sanitizador {

    private static final Pattern TAGS_HTML = Pattern.compile("<[^>]*>");
    private static final Pattern SEQUENCIAS_SQL = Pattern.compile("(--|;|/\\*|\\*/)");
    private static final Pattern ESPACOS_EXTRAS = Pattern.compile("\\s{2,}");

    private Sanitizador() {
    }

    public static String sanitizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = TAGS_HTML.matcher(texto).replaceAll("");
        limpo = SEQUENCIAS_SQL.matcher(limpo).replaceAll("");
        limpo = ESPACOS_EXTRAS.matcher(limpo).replaceAll(" ");
        return limpo.trim();
    }
}