package com.adobe.aem.modernizer.agent;

import org.osgi.service.component.annotations.Component;

import java.util.Locale;

/**
 * Intent classification service (Section 18).
 * Analyzes operator messages and determines the optimal retrieval strategy and tool chain.
 */
@Component(service = IntentService.class, immediate = true)
public class IntentService {

    public enum IntentType {
        EDS_QUESTION,           // Inquiries about EDS blocks, models, authoring, syntax
        AEM_FACT_QUERY,         // Direct queries on live AEM pages, components, templates
        MIGRATION_DIAGNOSTIC,   // Explaining why a page or component failed, validation logs
        ACTION_PROPOSAL,        // Asking to migrate, dry-run, create mapping, or edit state
        GENERAL_CHAT            // General greetings or guidance
    }

    public IntentType classify(String message) {
        if (message == null || message.isBlank()) {
            return IntentType.GENERAL_CHAT;
        }

        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("why did") || lower.contains("failed") || lower.contains("validation")
                || lower.contains("diagnostic") || lower.contains("error")) {
            return IntentType.MIGRATION_DIAGNOSTIC;
        }

        if (lower.startsWith("migrate") || lower.startsWith("run dry") || lower.contains("start migration")
                || lower.contains("create mapping") || lower.contains("apply fix")) {
            return IntentType.ACTION_PROPOSAL;
        }

        if (lower.contains("/content/") || lower.contains("/apps/") || lower.contains("jcr:")
                || lower.contains("sling:resourcetype") || lower.contains("page properties")) {
            return IntentType.AEM_FACT_QUERY;
        }

        if (lower.contains("block") || lower.contains("hero") || lower.contains("cards")
                || lower.contains("teaser") || lower.contains("author") || lower.contains("universal editor")
                || lower.contains("model") || lower.contains("fstab") || lower.contains("eds")) {
            return IntentType.EDS_QUESTION;
        }

        return IntentType.GENERAL_CHAT;
    }
}
