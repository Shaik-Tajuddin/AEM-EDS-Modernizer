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
    private String preferredProvider;
    private String preferredModel;
    private String projectId;
    private String jobId;
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

    public String getPreferredProvider() { return preferredProvider; }
    public void setPreferredProvider(String preferredProvider) { this.preferredProvider = preferredProvider; }

    public String getPreferredModel() { return preferredModel; }
    public void setPreferredModel(String preferredModel) { this.preferredModel = preferredModel; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public List<String> getImageBase64List() { return imageBase64List != null ? new ArrayList<>(imageBase64List) : new ArrayList<>(); }
    public void setImageBase64List(List<String> imageBase64List) { this.imageBase64List = imageBase64List != null ? new ArrayList<>(imageBase64List) : new ArrayList<>(); }
}
