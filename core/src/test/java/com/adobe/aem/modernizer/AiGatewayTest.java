package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.ModelCapability;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiGatewayTest {

    @Test
    void testDispatchThroughMockProvider() {
        AiGateway ai = new AiGateway();

        ChatRequest req = new ChatRequest("component-intelligence", "Analyze hero component");
        req.setTargetCapability(ModelCapability.CAP_STRUCTURED);

        ChatResponse resp = ai.dispatch(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getContent()).isNotEmpty();
        assertThat(resp.getProvider()).isEqualTo("mock");
        assertThat(resp.getTokenUsage().getTotalTokens()).isGreaterThan(0);
    }
}
