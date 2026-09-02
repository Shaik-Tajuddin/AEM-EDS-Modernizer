package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata descriptor for knowledge documents and chunks, supporting multi-tenant isolation,
 * source classification, and security policies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String projectId;
    private String environment;
    private String classification; // PUBLIC, INTERNAL, RESTRICTED, CONFIDENTIAL
    private String authoringContext; // BLOCK, TEMPLATE, MODEL, RULE, CONFIG, SYSTEM
    private boolean isGlobal;
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public KnowledgeMetadata() {
    }

    public KnowledgeMetadata(String projectId, String classification, String authoringContext) {
        this.projectId = projectId;
        this.classification = classification;
        this.authoringContext = authoringContext;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getAuthoringContext() {
        return authoringContext;
    }

    public void setAuthoringContext(String authoringContext) {
        this.authoringContext = authoringContext;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
    }

    public void addAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap<>();
        }
        this.attributes.put(key, value);
    }
}
