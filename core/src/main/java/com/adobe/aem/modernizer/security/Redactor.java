package com.adobe.aem.modernizer.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips secrets, tokens, credentials, and basic auth from logs and API responses (ADR 0014).
 */
public final class Redactor {

    private static final List<Pattern> PATTERNS = new ArrayList<>();

    static {
        // GitHub tokens
        PATTERNS.add(Pattern.compile("(?i)\\b(gh[pousr]_[A-Za-z0-9_]{16,255})\\b"));
        // OpenAI API keys
        PATTERNS.add(Pattern.compile("\\b(sk-[A-Za-z0-9_-]{20,255})\\b"));
        // Anthropic API keys
        PATTERNS.add(Pattern.compile("\\b(sk-ant-[A-Za-z0-9_-]{20,255})\\b"));
        // Google AI Studio / Gemini keys
        PATTERNS.add(Pattern.compile("\\b(AIza[A-Za-z0-9_-]{35})\\b"));
        // Figma tokens
        PATTERNS.add(Pattern.compile("\\b(figd_[A-Za-z0-9_-]{20,255})\\b"));
        // Authorization headers
        PATTERNS.add(Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9_\\-\\.]+"));
        // Basic auth URLs: http(s)://user:pass@host
        PATTERNS.add(Pattern.compile("://[^/\\s:@]+:[^/\\s@]+@"));
    }

    private Redactor() {}

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(result);
            if (m.find()) {
                result = m.replaceAll("[REDACTED]");
            }
        }
        return result;
    }
}
