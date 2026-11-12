package com.substream.domain;

/**
 * M1. BR-102 email rules, implemented with plain String operations (no regular expressions).
 */
public final class EmailValidator {

    private EmailValidator() {
    }

    /** BR-102: {@code null} and blank values are invalid. */
    public static boolean isValid(String email) {
        // TODO: BR-102
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isWhitespace(trimmed.charAt(i))) {
                return false;
            }
        }
        int at = trimmed.indexOf('@');
        return at > 0
                && at == trimmed.lastIndexOf('@')
                && trimmed.indexOf('.', at + 1) > at;
    }
}
