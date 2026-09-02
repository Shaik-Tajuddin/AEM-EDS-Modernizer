package com.adobe.aem.modernizer.agent.tools;

/**
 * Common abstraction for tools callable by Modernizer AI Agents (Section 20).
 */
public interface AgentTool {

    /** Unique tool name (e.g. "getPage", "searchKnowledge", "runDryRun"). */
    String getName();

    /** Human-readable description of what the tool accomplishes. */
    String getDescription();

    /** JSON Schema for parameter validation. */
    ToolSchema getSchema();

    /** Risk classification (READ, WRITE, HIGH_RISK). */
    RiskLevel getRiskLevel();

    /** Whether execution strictly requires explicit user confirmation. */
    default boolean requiresConfirmation() {
        return getRiskLevel() != RiskLevel.READ;
    }

    /** Executes the tool logic within the authenticated context. */
    ToolResult execute(ToolContext context);
}
