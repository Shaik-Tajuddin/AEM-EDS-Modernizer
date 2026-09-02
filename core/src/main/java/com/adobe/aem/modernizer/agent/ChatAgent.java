package com.adobe.aem.modernizer.agent;

import com.adobe.aem.modernizer.agent.tools.ToolContext;
import com.adobe.aem.modernizer.agent.tools.ToolRegistry;
import com.adobe.aem.modernizer.agent.tools.ToolResult;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.retrieval.CitationService;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalRequest;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalResponse;
import com.adobe.aem.modernizer.rag.retrieval.RetrievalService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enterprise Production Chat Agent (Section 18).
 * Grounded in AEM repository facts, EDS codebase knowledge, and migration history.
 */
@Component(service = ChatAgent.class, immediate = true)
public class ChatAgent {

    private static final Logger LOG = LoggerFactory.getLogger(ChatAgent.class);

    @Reference
    private transient IntentService intentService;

    @Reference
    private transient RetrievalService retrievalService;

    @Reference
    private transient ContextBuilder contextBuilder;

    @Reference
    private transient AiGateway aiGateway;

    @Reference
    private transient ToolRegistry toolRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient Store store;

    @Reference
    private transient CitationService citationService;

    public ChatAgent() {
    }

    public ChatAgent(IntentService intentService,
                     RetrievalService retrievalService,
                     ContextBuilder contextBuilder,
                     AiGateway aiGateway,
                     ToolRegistry toolRegistry,
                     CitationService citationService) {
        this.intentService = intentService;
        this.retrievalService = retrievalService;
        this.contextBuilder = contextBuilder;
        this.aiGateway = aiGateway;
        this.toolRegistry = toolRegistry;
        this.citationService = citationService;
    }

    public Map<String, Object> handleChat(String projectId, String userId, String message, String conversationId) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new LinkedHashMap<>();

        if (message == null || message.isBlank()) {
            response.put("answer", "Please enter a message to begin.");
            response.put("confidence", 0.0);
            response.put("citations", Collections.emptyList());
            response.put("suggestedActions", Collections.emptyList());
            return response;
        }

        String targetProject = (projectId != null && !projectId.isBlank()) ? projectId : "default";

        // 1. Intent Detection
        IntentService.IntentType intent = (intentService != null)
                ? intentService.classify(message) : IntentService.IntentType.GENERAL_CHAT;

        LOG.info("ChatAgent processing query for project '{}' [Intent: {}]: {}", targetProject, intent, message);

        // 2. Optional Live Tool Pre-Execution for AEM Facts & Diagnostics
        StringBuilder toolOutput = new StringBuilder();
        if (intent == IntentService.IntentType.AEM_FACT_QUERY || intent == IntentService.IntentType.MIGRATION_DIAGNOSTIC) {
            if (toolRegistry != null) {
                ToolContext toolCtx = new ToolContext(targetProject, userId, Map.of("jobId", "latest"));
                ToolResult res = toolRegistry.execute("getValidationResults", toolCtx);
                if (res != null && res.getData() != null) {
                    toolOutput.append("Latest Validation Status: ").append(res.getData().toString()).append("\n");
                }
            }
        }

        // 3. Hybrid RAG Knowledge Retrieval
        RetrievalRequest retReq = new RetrievalRequest(message, targetProject);
        retReq.setUserId(userId);
        retReq.setTopK(6);

        RetrievalResponse retRes = (retrievalService != null)
                ? retrievalService.retrieve(retReq)
                : new RetrievalResponse(message, targetProject);

        // 4. Build Fenced Context
        String projectPolicy = resolveProjectPolicy(targetProject);
        String prompt = (contextBuilder != null)
                ? contextBuilder.buildPrompt(message, projectPolicy, retRes.getResults(), toolOutput.toString())
                : message;

        // 5. LLM Completion via AI Gateway
        String answer;
        String providerUsed = "mock";
        String modelUsed = "default";

        try {
            ChatRequest chatReq = new ChatRequest("chat-agent", prompt);
            chatReq.setProjectId(targetProject);
            chatReq.setMaxTokens(2048);

            ProjectRecord proj = (store != null) ? store.getProject(targetProject).orElse(null) : null;
            if (proj != null && proj.getAiProvider() != null) {
                chatReq.setPreferredProvider(proj.getAiProvider());
                chatReq.setPreferredModel(proj.getAiModel());
            }

            ChatResponse chatRes = (aiGateway != null) ? aiGateway.dispatch(chatReq) : null;
            if (chatRes != null && chatRes.getContent() != null && !chatRes.getContent().isBlank()) {
                answer = chatRes.getContent().trim();
                providerUsed = chatRes.getProvider();
                modelUsed = chatRes.getModelName();
            } else {
                answer = generateGroundedFallback(message, retRes);
            }
        } catch (Exception e) {
            LOG.warn("AI Gateway invocation failed, providing grounded repository fallback: {}", e.getMessage());
            answer = generateGroundedFallback(message, retRes);
        }

        // 6. Suggest Actions (Policy-safe)
        List<Map<String, Object>> suggestedActions = generateSuggestedActions(intent, targetProject, message);

        response.put("answer", answer);
        response.put("confidence", retRes.getConfidenceScore());
        response.put("confidenceLevel", retRes.getConfidenceLevel());
        response.put("citations", retRes.getCitations());
        response.put("retrieval", Map.of(
                "totalFound", retRes.getTotalDiscovered(),
                "chunksRetrieved", retRes.getResults().size(),
                "durationMs", retRes.getExecutionDurationMs()
        ));
        response.put("suggestedActions", suggestedActions);
        response.put("provider", providerUsed);
        response.put("model", modelUsed);
        response.put("durationMs", System.currentTimeMillis() - startTime);

        return response;
    }

    private String resolveProjectPolicy(String projectId) {
        if (store != null) {
            Optional<ProjectRecord> p = store.getProject(projectId);
            if (p.isPresent()) {
                ProjectRecord proj = p.get();
                return "Project: " + proj.getName() + "\nAEM Content Root: " + proj.getContentRoot() +
                        "\nEDS Target Repository: " + proj.getEdsGitRepoUrl() + "\nEDS Branch: " + proj.getEdsBranch();
            }
        }
        return "Standard AEM to EDS Modernization Project.";
    }

    private String generateGroundedFallback(String message, RetrievalResponse retRes) {
        StringBuilder sb = new StringBuilder();
        if (retRes.getResults().isEmpty()) {
            sb.append("Based on the repository and project index, no matching information was found for your query.\n");
            sb.append("Please verify that the EDS repository synchronization has completed.");
        } else {
            sb.append("Based on the EDS repository and project rules:\n\n");
            for (var r : retRes.getResults()) {
                if (r.getChunk() != null && r.getChunk().getContent() != null) {
                    sb.append("• **").append(r.getChunk().getHeading()).append("**: ")
                            .append(r.getChunk().getContent().lines().findFirst().orElse("")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private List<Map<String, Object>> generateSuggestedActions(IntentService.IntentType intent, String projectId, String message) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (intent == IntentService.IntentType.MIGRATION_DIAGNOSTIC || intent == IntentService.IntentType.ACTION_PROPOSAL) {
            Map<String, Object> dryRunAction = new LinkedHashMap<>();
            dryRunAction.put("id", "action-dry-run");
            dryRunAction.put("label", "Run Dry Run Migration");
            dryRunAction.put("tool", "runDryRun");
            dryRunAction.put("params", Map.of("projectId", projectId));
            dryRunAction.put("riskLevel", "WRITE");
            dryRunAction.put("requiresConfirmation", true);
            actions.add(dryRunAction);

            Map<String, Object> statusAction = new LinkedHashMap<>();
            statusAction.put("id", "action-status");
            statusAction.put("label", "Check Validation Results");
            statusAction.put("tool", "getValidationResults");
            statusAction.put("params", Map.of("projectId", projectId));
            statusAction.put("riskLevel", "READ");
            statusAction.put("requiresConfirmation", false);
            actions.add(statusAction);
        } else if (intent == IntentService.IntentType.EDS_QUESTION) {
            Map<String, Object> syncAction = new LinkedHashMap<>();
            syncAction.put("id", "action-sync-repo");
            syncAction.put("label", "Resync EDS Knowledge");
            syncAction.put("tool", "syncKnowledge");
            syncAction.put("params", Map.of("projectId", projectId));
            syncAction.put("riskLevel", "WRITE");
            syncAction.put("requiresConfirmation", false);
            actions.add(syncAction);
        }
        return actions;
    }
}
