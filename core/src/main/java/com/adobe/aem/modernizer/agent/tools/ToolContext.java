package com.adobe.aem.modernizer.agent.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Execution context provided when invoking an AgentTool.
 */
public class ToolContext {

    private String projectId;
    private String userId;
    private String userRole = "author";
    private Map<String, Object> arguments = new LinkedHashMap<>();

    public ToolContext() {
    }

    public ToolContext(String projectId, String userId, Map<String, Object> arguments) {
        this.projectId = projectId;
        this.userId = userId;
        this.arguments = arguments != null ? arguments : new LinkedHashMap<>();
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public Map<String, Object> getArguments() {
        return arguments != null ? arguments : Collections.emptyMap();
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments != null ? new LinkedHashMap<>(arguments) : new LinkedHashMap<>();
    }

    public String getString(String key) {
        Object val = arguments.get(key);
        return val != null ? val.toString() : null;
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        Object val = arguments.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }
}
