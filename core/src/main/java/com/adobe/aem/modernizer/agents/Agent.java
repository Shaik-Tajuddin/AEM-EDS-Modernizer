package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ModernizerException;

/**
 * Common interface for all Phase 1 and Phase 2 Modernizer agents.
 */
public interface Agent {

    String getName();

    MigrationState getStage();

    void execute(AgentContext ctx) throws ModernizerException;
}
