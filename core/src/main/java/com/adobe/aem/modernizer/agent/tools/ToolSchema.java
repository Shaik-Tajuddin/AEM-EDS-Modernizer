package com.adobe.aem.modernizer.agent.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON Schema parameter declaration for an agent tool.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type = "object";
    private Map<String, Object> properties = new LinkedHashMap<>();
    private String[] required = new String[0];

    public ToolSchema() {
    }

    public ToolSchema(Map<String, Object> properties, String... required) {
        this.properties = properties != null ? properties : new LinkedHashMap<>();
        this.required = required != null ? required : new String[0];
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String[] getRequired() {
        return required;
    }

    public void setRequired(String[] required) {
        this.required = required;
    }
}
