package com.adobe.aem.modernizer.ai;

import java.util.Locale;

/**
 * Local IDE agents that hand off block generation (no cloud API key).
 * Cloud Google API remains provider id {@code gemini}; IDE Gemini is {@code geminicode}.
 */
public final class IdeAgentProviders {

    public static final String CLAUDE_CODE = "claudecode";
    public static final String GEMINI_CODE = "geminicode";
    public static final String CURSOR = "cursor";
    public static final String ANTIGRAVITY = "antigravity";

    private IdeAgentProviders() {}

    public static boolean isIdeAgent(String provider) {
        return isLocalOnlyProvider(provider);
    }

    public static boolean isLocalOnlyProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        String p = provider.trim().toLowerCase(Locale.ROOT);
        return CLAUDE_CODE.equals(p)
                || CURSOR.equals(p)
                || ANTIGRAVITY.equals(p)
                || GEMINI_CODE.equals(p);
    }

    public static String displayName(String provider) {
        if (provider == null) {
            return "IDE Agent";
        }
        switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case CLAUDE_CODE:
                return "Claude Code";
            case CURSOR:
                return "Cursor";
            case ANTIGRAVITY:
                return "Antigravity";
            case GEMINI_CODE:
                return "Gemini IDE";
            default:
                return provider;
        }
    }
}
