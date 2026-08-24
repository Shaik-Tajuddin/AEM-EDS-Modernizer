package com.adobe.aem.modernizer.ai;

/**
 * Standardized AI Chat Response.
 */
public class ChatResponse {

    private String content;
    private String modelName;
    private String provider;
    private TokenUsage tokenUsage = new TokenUsage();
    private double costUsd;
    private String finishReason;

    public ChatResponse() {}

    public ChatResponse(String content, String provider, String modelName) {
        this.content = content;
        this.provider = provider;
        this.modelName = modelName;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }

    public double getCostUsd() { return costUsd; }
    public void setCostUsd(double costUsd) { this.costUsd = costUsd; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
}
