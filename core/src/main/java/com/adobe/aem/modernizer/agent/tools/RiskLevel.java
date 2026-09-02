package com.adobe.aem.modernizer.agent.tools;

/**
 * Risk classification for agent tool execution (Sections 20, 21).
 */
public enum RiskLevel {
    READ,       // Safe, read-only queries (no confirmation needed)
    WRITE,      // Creates draft mappings, plans, clarifications (requires confirmation)
    HIGH_RISK   // Bulk migration, publish, delete (requires explicit confirmation and audit logging)
}
