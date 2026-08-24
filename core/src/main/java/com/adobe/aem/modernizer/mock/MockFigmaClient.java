package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.FigmaClient;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock Figma Client returning design tokens.
 */
@Component(service = FigmaClient.class, immediate = true)
public class MockFigmaClient implements FigmaClient {

    private String figmaUrl = "https://www.figma.com/design/abcdef/WKND";

    public MockFigmaClient() {}

    public MockFigmaClient(String figmaUrl) {
        this.figmaUrl = figmaUrl;
    }

    @Override
    public boolean testConnection() {
        return true;
    }

    @Override
    public String getFigmaUrl() {
        return figmaUrl;
    }

    @Override
    public Map<String, String> extractTokens(String figmaUrl) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("--color-brand", "#eb1000");
        tokens.put("--color-text", "#222222");
        tokens.put("--color-bg", "#ffffff");
        tokens.put("--color-accent", "#ffea00");
        tokens.put("--font-body", "'Source Sans Pro', sans-serif");
        tokens.put("--font-heading", "'Source Serif Pro', serif");
        tokens.put("--spacing-unit", "8px");
        return tokens;
    }
}
