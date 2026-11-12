/*
 * AQA TRACK ONLY. Developer-track students can ignore this package.
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * This is the system the AQA track tests. It may contain defects: finding them is your job.
 */
package com.substream.aqa.pricing.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON support for the Pricing API: writes objects and arrays,
 * and reads flat request objects ({"plan": "PRO", "country": "EG"}).
 */
final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------ writing

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, String.valueOf(entry.getKey()));
                out.append(':');
                writeValue(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeValue(out, list.get(i));
            }
            out.append(']');
        } else {
            writeString(out, value.toString());
        }
    }

    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------ reading

    /** Parses a flat JSON object. Values are String, Long, Double, Boolean or null. */
    static Map<String, Object> readObject(String text) {
        Parser parser = new Parser(text == null ? "" : text);
        Map<String, Object> result = parser.object();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Malformed JSON: unexpected content after the object");
        }
        return result;
    }

    private static final class Parser {

        private final String text;
        private int position;

        Parser(String text) {
            this.text = text;
        }

        Map<String, Object> object() {
            skipWhitespace();
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                position++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                result.put(key, value());
                skipWhitespace();
                char next = next();
                if (next == '}') {
                    return result;
                }
                if (next != ',') {
                    throw malformed();
                }
            }
        }

        private Object value() {
            char c = peek();
            if (c == '"') {
                return string();
            }
            if (text.startsWith("true", position)) {
                position += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", position)) {
                position += 5;
                return Boolean.FALSE;
            }
            if (text.startsWith("null", position)) {
                position += 4;
                return null;
            }
            if (c == '-' || Character.isDigit(c)) {
                return number();
            }
            throw new IllegalArgumentException("Malformed JSON: only flat objects with simple values are supported");
        }

        private Object number() {
            int start = position;
            while (!atEnd() && "+-.eE0123456789".indexOf(peek()) >= 0) {
                position++;
            }
            String literal = text.substring(start, position);
            try {
                return literal.matches("-?\\d+") ? (Object) Long.parseLong(literal) : (Object) Double.parseDouble(literal);
            } catch (NumberFormatException e) {
                throw malformed();
            }
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escaped = next();
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        if (position + 4 > text.length()) {
                            throw malformed();
                        }
                        out.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                        position += 4;
                    }
                    default -> throw malformed();
                }
            }
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(peek())) {
                position++;
            }
        }

        boolean atEnd() {
            return position >= text.length();
        }

        private char peek() {
            if (atEnd()) {
                throw malformed();
            }
            return text.charAt(position);
        }

        private char next() {
            char c = peek();
            position++;
            return c;
        }

        private void expect(char expected) {
            if (next() != expected) {
                throw malformed();
            }
        }

        private IllegalArgumentException malformed() {
            return new IllegalArgumentException("Malformed JSON at position " + position);
        }
    }
}
