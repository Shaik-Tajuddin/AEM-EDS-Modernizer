package com.adobe.aem.modernizer.ai;

/**
 * Token usage accounting.
 */
public class TokenUsage {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public TokenUsage() {}

    public TokenUsage(int promptTokens, int completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = promptTokens + completionTokens;
    }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
        this.totalTokens = this.promptTokens + this.completionTokens;
    }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
        this.totalTokens = this.promptTokens + this.completionTokens;
    }

    public int getTotalTokens() { return totalTokens; }
}
