package com.adobe.aem.modernizer.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Standardized AI Chat Request.
 */
public class ChatRequest {

    private String agentName;
    private String prompt;
    private String systemPrompt;
    private String targetCapability; // e.g. CAP_CODE, CAP_STRUCTURED, CAP_VISION
    private String responseSchema;
    private double temperature = 0.2;
    private int maxTokens = 4096;
    private List<String> imageBase64List = new ArrayList<>();

    public ChatRequest() {}

    public ChatRequest(String agentName, String prompt) {
        this.agentName = agentName;
        this.prompt = prompt;
    }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getTargetCapability() { return targetCapability; }
    public void setTargetCapability(String targetCapability) { this.targetCapability = targetCapability; }

    public String getResponseSchema() { return responseSchema; }
    public void setResponseSchema(String responseSchema) { this.responseSchema = responseSchema; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public List<String> getImageBase64List() { return imageBase64List; }
    public void setImageBase64List(List<String> imageBase64List) { this.imageBase64List = imageBase64List; }
}
