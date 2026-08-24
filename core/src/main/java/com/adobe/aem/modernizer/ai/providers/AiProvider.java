package com.adobe.aem.modernizer.ai.providers;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;

/**
 * Common abstraction for all AI providers (Master §16).
 */
public interface AiProvider {

    String getProviderName();

    ChatResponse chat(ChatRequest request, String model, String apiKey);

    boolean isAvailable();
}
