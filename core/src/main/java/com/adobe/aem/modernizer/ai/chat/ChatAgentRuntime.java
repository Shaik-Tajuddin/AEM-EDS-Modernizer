package com.adobe.aem.modernizer.ai.chat;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatPracticeContext;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.IdeAgentProviders;
import com.adobe.aem.modernizer.ai.providers.AiProviderException;
import com.adobe.aem.modernizer.agents.Orchestrator;
import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cursor-like bounded agent loop for dashboard chat: plan → tools → reply.
 */
public class ChatAgentRuntime {

    private static final Logger LOG = LoggerFactory.getLogger(ChatAgentRuntime.class);
    private static final int MAX_STEPS = 10;
    private static final Pattern TOOL_LINE = Pattern.compile(
            "(?i)^\\s*TOOL\\s*:\\s*([a-z0-9_]+)\\s*(?:\\|(.*))?$");

    private final AiGateway aiGateway;
    private final ChatToolRegistry tools;

    public ChatAgentRuntime(AiGateway aiGateway, Store store, Orchestrator orchestrator, GitHubClient gitHubClient) {
        this.aiGateway = aiGateway;
        this.tools = new ChatToolRegistry(store, orchestrator, gitHubClient);
    }

    public Map<String, Object> handle(String projectId, ProjectRecord project, String message, String historyText) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();
        result.put("steps", steps);

        if (message == null || message.isBlank()) {
            result.put("reply", "Please send a message.");
            result.put("provider", "none");
            result.put("model", "none");
            return result;
        }

        String provider = project != null ? project.getAiProvider() : "mock";
        String model = project != null ? project.getAiModel() : null;
        boolean localViaOllama = false;

        // Local / IDE project settings: Agent Chat talks through Ollama (local LLM), not cloud APIs.
        // Block generation still uses IDE handoff separately in BlockGenerationAgent.
        if (IdeAgentProviders.isLocalOnlyProvider(provider) || "ollama".equalsIgnoreCase(provider)) {
            localViaOllama = true;
            provider = "ollama";
            if (model == null || model.isBlank()) {
                String osgiDefault = null;
                if (aiGateway != null && aiGateway.endpoints() != null
                        && aiGateway.endpoints().get("ollama") != null) {
                    osgiDefault = aiGateway.endpoints().get("ollama").getDefaultModel();
                }
                model = (osgiDefault != null && !osgiDefault.isBlank()) ? osgiDefault : "qwen3:8b";
            }
        }

        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }

        StringBuilder transcript = new StringBuilder();
        if (historyText != null) {
            transcript.append(historyText).append('\n');
        }
        transcript.append("Operator: ").append(message).append('\n');

        String reply = "";
        String usedProvider = provider;
        String usedModel = model != null ? model : "default";

        try {
            for (int step = 0; step < MAX_STEPS; step++) {
                String prompt = buildPlannerPrompt(projectId, project, transcript.toString(), step == 0);
                ChatRequest req = new ChatRequest("dashboard-assistant", prompt);
                req.setSystemPrompt(ChatPracticeContext.systemPrompt());
                req.setPreferredProvider(provider);
                req.setPreferredModel(model);
                req.setProjectId(projectId);
                req.setMaxTokens(2048);

                ChatResponse chatRes = aiGateway.dispatch(req);
                usedProvider = chatRes.getProvider() != null ? chatRes.getProvider() : usedProvider;
                usedModel = chatRes.getModelName() != null ? chatRes.getModelName() : usedModel;
                String content = chatRes.getContent() != null ? chatRes.getContent().trim() : "";

                Matcher m = TOOL_LINE.matcher(firstLine(content));
                if (m.find()) {
                    String toolName = m.group(1);
                    Map<String, Object> args = parseArgs(m.group(2));
                    String toolOut = tools.execute(projectId, toolName, args);
                    Map<String, Object> stepInfo = new LinkedHashMap<>();
                    stepInfo.put("tool", toolName);
                    stepInfo.put("args", args);
                    stepInfo.put("ok", !toolOut.contains("\"ok\":false"));
                    steps.add(stepInfo);
                    transcript.append("Agent planned TOOL:").append(toolName).append('\n');
                    transcript.append("ToolResult: ").append(toolOut).append('\n');
                    continue;
                }

                reply = stripToolNoise(content);
                break;
            }
            if (reply == null || reply.isBlank()) {
                reply = "I ran " + steps.size() + " tool step(s). Ask if you need more detail.";
            }
        } catch (AiProviderException e) {
            LOG.warn("Chat agent provider error: {}", e.getMessage());
            if (localViaOllama) {
                reply = "Local Ollama is unavailable (" + e.getMessage()
                        + "). Start Ollama (e.g. `ollama serve` / pull model `" + usedModel
                        + "`), or switch Project Setup to a cloud provider.\n\n"
                        + handleLocalHeuristic(projectId, message, steps);
            } else {
                reply = "AI provider error: " + e.getMessage();
                if (steps.isEmpty()) {
                    reply = handleLocalHeuristic(projectId, message, steps) + "\n\n(" + reply + ")";
                }
            }
        } catch (Exception e) {
            LOG.warn("Chat agent failed: {}", e.getMessage());
            reply = "Sorry, the agent could not finish: " + e.getMessage();
        }

        result.put("reply", reply);
        result.put("provider", usedProvider);
        result.put("model", usedModel);
        return result;
    }

    private String handleLocalHeuristic(String projectId, String message, List<Map<String, Object>> steps) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("dry run") || lower.contains("dry-run")) {
            String out = tools.execute(projectId, "run_dry_run", Map.of());
            steps.add(Map.of("tool", "run_dry_run", "ok", true));
            return "Started dry run.\n" + out;
        }
        if (lower.contains("migrate") && !lower.contains("migration plan")) {
            String out = tools.execute(projectId, "run_migrate", Map.of());
            steps.add(Map.of("tool", "run_migrate", "ok", true));
            return "Started migration.\n" + out;
        }
        if (lower.contains("block")) {
            String out = tools.execute(projectId, "list_blocks", Map.of());
            steps.add(Map.of("tool", "list_blocks", "ok", true));
            return "Here are the blocks I can see:\n" + out;
        }
        if (lower.contains("event") || lower.contains("status") || lower.contains("overview")) {
            String out = tools.execute(projectId, "project_status", Map.of());
            steps.add(Map.of("tool", "project_status", "ok", true));
            return "Project status:\n" + out;
        }
        String status = tools.execute(projectId, "project_status", Map.of());
        steps.add(Map.of("tool", "project_status", "ok", true));
        return "Ollama unavailable — keyword tools only. Project status:\n" + status
                + "\nTry: list blocks, run dry run, show status.";
    }

    private static String buildPlannerPrompt(String projectId, ProjectRecord project, String transcript, boolean first) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project id: ").append(projectId).append('\n');
        if (project != null) {
            sb.append("Name: ").append(project.getName()).append('\n');
            sb.append("AEM: ").append(project.getAemAuthorUrl()).append('\n');
            sb.append("EDS repo: ").append(project.getEdsGitRepoUrl()).append('\n');
            sb.append("AI provider: ").append(project.getAiProvider()).append('\n');
        }
        sb.append("\nAvailable tools (emit exactly one line to call a tool):\n");
        sb.append("TOOL:project_status\n");
        sb.append("TOOL:list_events\n");
        sb.append("TOOL:list_blocks\n");
        sb.append("TOOL:read_tool_file|path=relative/path\n");
        sb.append("TOOL:read_eds_file|path=blocks/hero/hero.js\n");
        sb.append("TOOL:search_tool_repo|query=BlockGeneration\n");
        sb.append("TOOL:run_dry_run\n");
        sb.append("TOOL:run_migrate\n");
        sb.append("\nIf you can answer without tools, reply in natural language (no TOOL: line).\n");
        sb.append("If you need a tool, reply with ONLY one TOOL: line.\n\n");
        sb.append("=== TRANSCRIPT ===\n").append(transcript);
        if (first) {
            sb.append("\nDecide the next action now.");
        } else {
            sb.append("\nContinue: either another TOOL: line or the final user-facing answer.");
        }
        return sb.toString();
    }

    private static String firstLine(String content) {
        if (content == null) return "";
        int nl = content.indexOf('\n');
        return nl < 0 ? content.trim() : content.substring(0, nl).trim();
    }

    private static Map<String, Object> parseArgs(String raw) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return args;
        }
        for (String part : raw.split("\\|")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int eq = p.indexOf('=');
            if (eq > 0) {
                args.put(p.substring(0, eq).trim(), p.substring(eq + 1).trim());
            }
        }
        return args;
    }

    private static String stripToolNoise(String content) {
        if (content == null) return "";
        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (TOOL_LINE.matcher(line).matches()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(line);
        }
        return sb.toString().trim();
    }
}
