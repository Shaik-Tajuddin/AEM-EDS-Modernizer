package com.adobe.aem.modernizer.agent.servlets;

import com.adobe.aem.modernizer.agent.ChatAgent;
import com.adobe.aem.modernizer.agent.tools.ToolContext;
import com.adobe.aem.modernizer.agent.tools.ToolRegistry;
import com.adobe.aem.modernizer.agent.tools.ToolResult;
import com.adobe.aem.modernizer.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Production REST endpoint for the RAG-grounded AI Chat Agent (Section 33):
 * {@code POST /bin/modernizer/chat}
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.paths=/bin/modernizer/chat",
        "sling.servlet.methods=POST"
})
@SlingServletPaths(value = {
        "/bin/modernizer/chat",
        "/bin/modernizer/chat/"
})
public class ChatServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ChatServlet.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private transient ChatAgent chatAgent;

    @Reference
    private transient ToolRegistry toolRegistry;

    public ChatServlet() {
    }

    public ChatServlet(ChatAgent chatAgent, ToolRegistry toolRegistry) {
        this.chatAgent = chatAgent;
        this.toolRegistry = toolRegistry;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String message = request.getParameter("message");
        if (message == null || message.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Parameter 'message' is required\"}");
            return;
        }
        String projectId = request.getParameter("projectId");
        if (projectId == null || projectId.isBlank()) projectId = "wknd-site";
        String conversationId = request.getParameter("conversationId");
        if (conversationId == null) conversationId = "conv-" + System.currentTimeMillis();

        String actor = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "admin";
        Map<String, Object> result = chatAgent.handleChat(projectId, actor, message, conversationId);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getWriter().write(JsonUtil.toJson(result));
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String body = sb.toString();
        if (body.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Request body cannot be empty\"}");
            return;
        }

        String projectId = "default";
        String message = "";
        String conversationId = "conv-" + System.currentTimeMillis();
        String actionToExecute = null;
        boolean confirmed = false;

        try {
            JsonNode json = MAPPER.readTree(body);
            if (json.has("projectId")) projectId = json.get("projectId").asText();
            if (json.has("message")) message = json.get("message").asText();
            if (json.has("conversationId")) conversationId = json.get("conversationId").asText();
            if (json.has("executeAction")) actionToExecute = json.get("executeAction").asText();
            if (json.has("confirmed")) confirmed = json.get("confirmed").asBoolean();
        } catch (Exception e) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}");
            return;
        }

        String userId = request.getResourceResolver() != null ? request.getResourceResolver().getUserID() : "author";

        // Handle confirmed tool execution if requested
        if (actionToExecute != null && !actionToExecute.isBlank()) {
            if (toolRegistry != null) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("confirmed", confirmed);
                args.put("projectId", projectId);

                ToolContext toolCtx = new ToolContext(projectId, userId, args);
                ToolResult tr = toolRegistry.execute(actionToExecute, toolCtx);
                response.getWriter().write(JsonUtil.toJson(tr));
                return;
            }
        }

        // Standard Chat Dispatch
        if (chatAgent != null) {
            Map<String, Object> chatRes = chatAgent.handleChat(projectId, userId, message, conversationId);
            response.getWriter().write(JsonUtil.toJson(chatRes));
        } else {
            response.setStatus(503);
            response.getWriter().write("{\"error\":\"ChatAgent service not available\"}");
        }
    }
}
