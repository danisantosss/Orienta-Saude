package com.orientasaude.util;

import java.util.regex.Pattern;

/**
 * Sanitizes free text entered by the user before it is persisted.
 * Strips HTML tags and sequences commonly used in SQL injection attempts,
 * without rejecting the submission (it only cleans the content).
 */
public final class Sanitizer {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern SQL_SEQUENCES = Pattern.compile("(--|;|/\\*|\\*/)");
    private static final Pattern EXTRA_WHITESPACE = Pattern.compile("\\s{2,}");

    private Sanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null) {
            return null;
        }
        String clean = HTML_TAGS.matcher(text).replaceAll("");
        clean = SQL_SEQUENCES.matcher(clean).replaceAll("");
        clean = EXTRA_WHITESPACE.matcher(clean).replaceAll(" ");
        return clean.trim();
    }
}
