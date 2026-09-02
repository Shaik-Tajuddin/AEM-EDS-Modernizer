package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.BlockReconcileHelper;
import com.adobe.aem.modernizer.ai.AiExecutionMode;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.IdeAgentProviders;
import com.adobe.aem.modernizer.ai.chat.ChatAgentRuntime;
import com.adobe.aem.modernizer.ai.providers.AiProviderException;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdeCloudChatAuditTest {

    @Test
    void executionModeSplitsIdeAndCloud() {
        assertThat(AiExecutionMode.fromProvider("antigravity")).isEqualTo(AiExecutionMode.LOCAL);
        assertThat(AiExecutionMode.fromProvider("cursor")).isEqualTo(AiExecutionMode.LOCAL);
        assertThat(AiExecutionMode.fromProvider("claudecode")).isEqualTo(AiExecutionMode.LOCAL);
        assertThat(AiExecutionMode.fromProvider("geminicode")).isEqualTo(AiExecutionMode.LOCAL);
        assertThat(AiExecutionMode.fromProvider("anthropic")).isEqualTo(AiExecutionMode.CLOUD);
        assertThat(AiExecutionMode.fromProvider("gemini")).isEqualTo(AiExecutionMode.CLOUD);
        assertThat(IdeAgentProviders.isLocalOnlyProvider("gemini")).isFalse();
    }

    @Test
    void reconcileDecideCreateLeaveEnhance() {
        assertThat(BlockReconcileHelper.decide("hero", Set.of(), null))
                .isEqualTo(BlockReconcileHelper.Action.CREATE);
        assertThat(BlockReconcileHelper.decide("hero", Set.of("hero"), null))
                .isEqualTo(BlockReconcileHelper.Action.LEAVE);
        assertThat(BlockReconcileHelper.decideWithMismatch("hero", Set.of("hero"), true))
                .isEqualTo(BlockReconcileHelper.Action.ENHANCE);
    }

    @Test
    void cloudMissingKeyDoesNotSilentMock() {
        AiGateway gateway = new AiGateway();
        ChatRequest req = new ChatRequest("dashboard-assistant", "hello");
        req.setPreferredProvider("anthropic");
        req.setPreferredModel("claude-3-5-sonnet-20241022");
        assertThatThrownBy(() -> gateway.dispatch(req))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void chatRuntimeLocalUsesOllama() {
        InMemoryStore store = new InMemoryStore();
        ProjectRecord pr = new ProjectRecord("p1", "Demo", "http://localhost:4502", "/content/x", "https://github.com/org/repo");
        pr.setAiProvider("cursor");
        store.saveProject(pr);
        ChatAgentRuntime runtime = new ChatAgentRuntime(new AiGateway(), store, null, null);
        Map<String, Object> out = runtime.handle("p1", pr, "show status", "");
        // Local IDE settings route chat to Ollama (or heuristic fallback if Ollama is down)
        assertThat(out.get("provider")).isEqualTo("ollama");
        assertThat(out.get("reply")).asString().isNotBlank();
    }
}
