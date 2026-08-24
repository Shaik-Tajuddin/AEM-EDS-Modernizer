package com.adobe.aem.modernizer.connectors;

import java.util.Map;

/**
 * Connector interface for Figma design token extraction and pairing.
 */
public interface FigmaClient {
    boolean testConnection();
    String getFigmaUrl();
    Map<String, String> extractTokens(String figmaUrl);
}
