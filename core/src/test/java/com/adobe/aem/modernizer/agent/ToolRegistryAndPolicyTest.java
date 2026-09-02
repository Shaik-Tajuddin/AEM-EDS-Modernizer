package com.adobe.aem.modernizer.agent;

import com.adobe.aem.modernizer.agent.security.PolicyEngine;
import com.adobe.aem.modernizer.agent.tools.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryAndPolicyTest {

    @Test
    void testPolicyEngineRiskGating() {
        PolicyEngine policy = new PolicyEngine();

        AgentTool readTool = new DummyTool("readTool", RiskLevel.READ);
        AgentTool writeTool = new DummyTool("writeTool", RiskLevel.WRITE);
        AgentTool highRiskTool = new DummyTool("highRiskTool", RiskLevel.HIGH_RISK);

        ToolContext unconfirmedCtx = new ToolContext("proj-1", "author1", Map.of("confirmed", false));
        ToolContext confirmedCtx = new ToolContext("proj-1", "author1", Map.of("confirmed", true));
        ToolContext adminConfirmedCtx = new ToolContext("proj-1", "admin1", Map.of("confirmed", true));
        adminConfirmedCtx.setUserRole("admin");

        // READ tools are always allowed
        assertThat(policy.canExecute(readTool, unconfirmedCtx)).isTrue();

        // WRITE tools require confirmation
        assertThat(policy.canExecute(writeTool, unconfirmedCtx)).isFalse();
        assertThat(policy.canExecute(writeTool, confirmedCtx)).isTrue();

        // HIGH_RISK tools require privileged role + confirmation
        assertThat(policy.canExecute(highRiskTool, confirmedCtx)).isFalse(); // author role lacks admin
        assertThat(policy.canExecute(highRiskTool, adminConfirmedCtx)).isTrue();
    }

    @Test
    void testToolRegistryConfirmationFlow() {
        ToolRegistry registry = new ToolRegistry();

        // Executing unconfirmed dry run requires confirmation
        ToolContext ctx = new ToolContext("proj-1", "author", Map.of("confirmed", false));
        ToolResult res = registry.execute("runDryRun", ctx);

        assertThat(res.isRequiresConfirmation()).isTrue();
        assertThat(res.getConfirmationPrompt()).contains("runDryRun");
    }

    private static class DummyTool implements AgentTool {
        private final String name;
        private final RiskLevel riskLevel;

        DummyTool(String name, RiskLevel riskLevel) {
            this.name = name;
            this.riskLevel = riskLevel;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return name; }
        @Override public ToolSchema getSchema() { return new ToolSchema(); }
        @Override public RiskLevel getRiskLevel() { return riskLevel; }
        @Override public ToolResult execute(ToolContext context) { return ToolResult.ok("Done"); }
    }
}
