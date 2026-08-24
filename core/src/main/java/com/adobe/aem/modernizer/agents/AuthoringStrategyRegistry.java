package com.adobe.aem.modernizer.agents;

import java.util.*;

/**
 * Registry of supported EDS Authoring Strategies (Master §13).
 */
public class AuthoringStrategyRegistry {

    public static final String UNIVERSAL_EDITOR = "UNIVERSAL_EDITOR";
    public static final String DOCUMENT_BASED = "DOCUMENT_BASED";
    public static final String EXISTING_REPO = "EXISTING_REPO";
    public static final String CUSTOM_ADAPTER = "CUSTOM_ADAPTER";

    private final Set<String> strategies = new HashSet<>(Arrays.asList(
            UNIVERSAL_EDITOR, DOCUMENT_BASED, EXISTING_REPO, CUSTOM_ADAPTER
    ));

    public boolean isValid(String strategy) {
        return strategy != null && strategies.contains(strategy.toUpperCase());
    }

    public Set<String> listStrategies() {
        return new HashSet<>(strategies);
    }
}
