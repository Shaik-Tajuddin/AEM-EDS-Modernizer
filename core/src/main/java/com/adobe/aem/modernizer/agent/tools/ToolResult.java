package com.adobe.aem.modernizer.agent.tools;

import com.adobe.aem.modernizer.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result returned from executing an AgentTool.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;
    private boolean requiresConfirmation;
    private String confirmationPrompt;
    private Map<String, Object> auditMetadata = new LinkedHashMap<>();

    public ToolResult() {
    }

    public static ToolResult ok(String message, Object data) {
        ToolResult r = new ToolResult();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static ToolResult ok(Object data) {
        return ok("Success", data);
    }

    public static ToolResult confirmationRequired(String prompt, Object draftData) {
        ToolResult r = new ToolResult();
        r.success = true;
        r.requiresConfirmation = true;
        r.confirmationPrompt = prompt;
        r.data = draftData;
        r.message = "Action requires operator confirmation.";
        return r;
    }

    public static ToolResult error(String message) {
        ToolResult r = new ToolResult();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public String getConfirmationPrompt() {
        return confirmationPrompt;
    }

    public void setConfirmationPrompt(String confirmationPrompt) {
        this.confirmationPrompt = confirmationPrompt;
    }

    public Map<String, Object> getAuditMetadata() {
        return auditMetadata;
    }

    public void setAuditMetadata(Map<String, Object> auditMetadata) {
        this.auditMetadata = auditMetadata;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
