package com.adobe.aem.modernizer.agent.security;

import com.adobe.aem.modernizer.agent.tools.AgentTool;
import com.adobe.aem.modernizer.agent.tools.RiskLevel;
import com.adobe.aem.modernizer.agent.tools.ToolContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enterprise Agent Security & Policy Engine (Sections 19, 21).
 * Enforces Role-Based Access Control (RBAC), risk gating, and operator confirmation workflows.
 */
@Component(service = PolicyEngine.class, immediate = true)
public class PolicyEngine {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyEngine.class);

    public boolean canExecute(AgentTool tool, ToolContext context) {
        if (tool == null || context == null) {
            return false;
        }

        String user = context.getUserId() != null ? context.getUserId() : "anonymous";
        String role = context.getUserRole() != null ? context.getUserRole() : "author";
        RiskLevel risk = tool.getRiskLevel();

        LOG.debug("Evaluating policy for user='{}', role='{}', tool='{}', risk='{}'",
                user, role, tool.getName(), risk);

        // Safe reads are allowed for authors and admins
        if (risk == RiskLevel.READ) {
            return true;
        }

        // Modifying operations require at least author role with confirmation
        if (risk == RiskLevel.WRITE) {
            boolean confirmed = context.getBoolean("confirmed", false);
            if (!confirmed) {
                LOG.info("Write tool [{}] blocked: explicit confirmation required from operator", tool.getName());
                return false;
            }
            return true;
        }

        // High-risk operations (bulk migrate, publish, delete) require admin/operator role and confirmation
        if (risk == RiskLevel.HIGH_RISK) {
            boolean confirmed = context.getBoolean("confirmed", false);
            boolean isPrivileged = "admin".equalsIgnoreCase(role) || "operator".equalsIgnoreCase(role);
            if (!isPrivileged) {
                LOG.warn("High-risk tool [{}] denied: user '{}' lacks privileged role (role={})",
                        tool.getName(), user, role);
                return false;
            }
            if (!confirmed) {
                LOG.info("High-risk tool [{}] blocked: awaiting explicit confirmation", tool.getName());
                return false;
            }
            return true;
        }

        return false;
    }
}
