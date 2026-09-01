package com.adobe.aem.modernizer.ai.providers;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.TokenUsage;

/**
 * Deterministic in-memory AI provider for offline testing, CI, and zero-cost verification.
 */
public class MockAiProvider implements AiProvider {

    private final String providerName;
    private final String defaultModel;

    public MockAiProvider() {
        this("mock", "mock-general-1");
    }

    public MockAiProvider(String providerName, String defaultModel) {
        this.providerName = providerName;
        this.defaultModel = defaultModel;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String model, String apiKey) {
        String agent = request.getAgentName() != null ? request.getAgentName() : "general";
        String chosenModel = model != null ? model : defaultModel;
        String content;
        String prompt = request.getPrompt() != null ? request.getPrompt() : "";
        String promptLower = prompt.toLowerCase();

        // Extract target block name from prompt
        String blockName = "content";
        if (promptLower.contains("breadcrumb")) blockName = "breadcrumb";
        else if (promptLower.contains("carousel")) blockName = "carousel";
        else if (promptLower.contains("tabs")) blockName = "tabs";
        else if (promptLower.contains("cards") || promptLower.contains("card")) blockName = "cards";
        else if (promptLower.contains("hero")) blockName = "hero";
        else if (promptLower.contains("title")) blockName = "title";
        else if (promptLower.contains("teaser")) blockName = "teaser";

        String titleCase = Character.toUpperCase(blockName.charAt(0)) + blockName.substring(1).replace('-', ' ');

        if ("component-intelligence".equalsIgnoreCase(agent) || "component-mapping".equalsIgnoreCase(agent)) {
            content = String.format("{\"proposedBlock\":\"%s\",\"variants\":[\"default\",\"emphasis\"],\"confidence\":0.98,\"classification\":\"SUPPORTED\"}", blockName);
        } else if ("block-generation".equalsIgnoreCase(agent)) {
            // Generates ES6 decorate() + createBlock() conforming to CREATE_AEM_BLOCK.md §4
            if (blockName.equals("carousel")) {
                content = "import { checkAndHandleNestedBlocks, replaceBlockRowsPreservingNestedBlocks, franklinBlockRow } from '../../scripts/utilities/block-helpers.js';\n\n"
                        + "export default async function decorate(block) {\n"
                        + "  await checkAndHandleNestedBlocks(block);\n"
                        + "  const slides = [...block.children];\n"
                        + "  const track = document.createElement('div');\n"
                        + "  track.classList.add('carousel-track');\n"
                        + "  slides.forEach((slide, idx) => {\n"
                        + "    const slideEl = document.createElement('div');\n"
                        + "    slideEl.classList.add('carousel-slide');\n"
                        + "    if (idx === 0) slideEl.classList.add('active');\n"
                        + "    slideEl.innerHTML = slide.innerHTML;\n"
                        + "    track.appendChild(slideEl);\n"
                        + "  });\n"
                        + "  replaceBlockRowsPreservingNestedBlocks(block, track);\n"
                        + "}\n\n"
                        + "export function createBlock(options = {}) {\n"
                        + "  return `<div class=\"carousel eds-block-carousel\">${franklinBlockRow(options.title || '')}</div>`;\n"
                        + "}\n";
            } else if (blockName.equals("tabs")) {
                content = "import { checkAndHandleNestedBlocks, replaceBlockRowsPreservingNestedBlocks, getTextFromBlockRow, getHtmlFromRow, franklinBlockRow } from '../../scripts/utilities/block-helpers.js';\n\n"
                        + "export default async function decorate(block) {\n"
                        + "  await checkAndHandleNestedBlocks(block);\n"
                        + "  const rows = [...block.children];\n"
                        + "  const tabList = document.createElement('div');\n"
                        + "  tabList.classList.add('tabs-list');\n"
                        + "  const panels = document.createElement('div');\n"
                        + "  panels.classList.add('tabs-panels');\n"
                        + "  rows.forEach((row, idx) => {\n"
                        + "    const title = getTextFromBlockRow(row.children[0]);\n"
                        + "    const content = getHtmlFromRow(row.children[1]);\n"
                        + "    const tabBtn = document.createElement('button');\n"
                        + "    tabBtn.classList.add('tab-btn');\n"
                        + "    if (idx === 0) tabBtn.classList.add('active');\n"
                        + "    tabBtn.textContent = title;\n"
                        + "    tabList.appendChild(tabBtn);\n"
                        + "    const panel = document.createElement('div');\n"
                        + "    panel.classList.add('tab-panel');\n"
                        + "    if (idx === 0) panel.classList.add('active');\n"
                        + "    panel.innerHTML = content;\n"
                        + "    panels.appendChild(panel);\n"
                        + "  });\n"
                        + "  block.innerHTML = '';\n"
                        + "  block.appendChild(tabList);\n"
                        + "  block.appendChild(panels);\n"
                        + "}\n\n"
                        + "export function createBlock(options = {}) {\n"
                        + "  return `<div class=\"tabs eds-block-tabs\">${franklinBlockRow(options.title || '')}</div>`;\n"
                        + "}\n";
            } else if (blockName.equals("breadcrumb")) {
                content = "import { checkAndHandleNestedBlocks, replaceBlockRowsPreservingNestedBlocks, getHtmlFromRow, franklinBlockRow } from '../../scripts/utilities/block-helpers.js';\n\n"
                        + "export default async function decorate(block) {\n"
                        + "  await checkAndHandleNestedBlocks(block);\n"
                        + "  const nav = document.createElement('nav');\n"
                        + "  nav.classList.add('breadcrumb-nav');\n"
                        + "  nav.innerHTML = getHtmlFromRow(block.firstElementChild) || '<a href=\"/\">Home</a>';\n"
                        + "  replaceBlockRowsPreservingNestedBlocks(block, nav);\n"
                        + "}\n\n"
                        + "export function createBlock(options = {}) {\n"
                        + "  return `<div class=\"breadcrumb eds-block-breadcrumb\">${franklinBlockRow(options.path || '')}</div>`;\n"
                        + "}\n";
            } else {
                content = "import {\n"
                        + "  checkAndHandleNestedBlocks,\n"
                        + "  replaceBlockRowsPreservingNestedBlocks,\n"
                        + "  getTextFromBlockRow,\n"
                        + "  getHtmlFromRow,\n"
                        + "  coerceAuthorClasses,\n"
                        + "  escapeHtml,\n"
                        + "  escapeHtmlAttribute,\n"
                        + "  franklinBlockRow,\n"
                        + "} from '../../scripts/utilities/block-helpers.js';\n\n"
                        + "function extractConfig(block) {\n"
                        + "  if (!block) return {};\n"
                        + "  const rows = [...block.children];\n"
                        + "  return {\n"
                        + "    id: getTextFromBlockRow(rows[0]),\n"
                        + "    title: getHtmlFromRow(rows[1]),\n"
                        + "    text: getHtmlFromRow(rows[2]),\n"
                        + "  };\n"
                        + "}\n\n"
                        + "export default async function decorate(block) {\n"
                        + "  await checkAndHandleNestedBlocks(block);\n"
                        + "  const config = extractConfig(block);\n"
                        + "  const inner = document.createElement('div');\n"
                        + "  inner.classList.add('" + blockName + "-inner');\n"
                        + "  if (config.title) {\n"
                        + "    const h = document.createElement('div');\n"
                        + "    h.classList.add('" + blockName + "-title');\n"
                        + "    h.innerHTML = config.title;\n"
                        + "    inner.appendChild(h);\n"
                        + "  }\n"
                        + "  if (config.text) {\n"
                        + "    const p = document.createElement('div');\n"
                        + "    p.classList.add('" + blockName + "-text');\n"
                        + "    p.innerHTML = config.text;\n"
                        + "    inner.appendChild(p);\n"
                        + "  }\n"
                        + "  replaceBlockRowsPreservingNestedBlocks(block, inner);\n"
                        + "  if (config.id) block.id = config.id;\n"
                        + "}\n\n"
                        + "export function createBlock(options = {}) {\n"
                        + "  const id = escapeHtml(options.id ?? '');\n"
                        + "  const title = typeof options.title === 'string' ? options.title : '';\n"
                        + "  const extra = coerceAuthorClasses(options.classes);\n"
                        + "  const rootClasses = ['" + blockName + "', 'eds-block-" + blockName + "', extra].filter(Boolean).join(' ');\n"
                        + "  return `<div class=\"${escapeHtmlAttribute(rootClasses)}\">${franklinBlockRow(id)}${franklinBlockRow(title)}</div>`;\n"
                        + "}\n";
            }
        } else if ("code-generation".equalsIgnoreCase(agent)) {
            // Generates Scoped CSS conforming to CREATE_AEM_BLOCK.md §5
            content = "." + blockName + " {\n"
                    + "  margin: var(--space-m, 32px) auto;\n"
                    + "  max-width: 1200px;\n"
                    + "  padding: 0 var(--space-s, 16px);\n"
                    + "  color: var(--color-base-text, #334155);\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-inner {\n"
                    + "  background: var(--color-base-background, #ffffff);\n"
                    + "  border-radius: var(--radius-md, 10px);\n"
                    + "  padding: var(--space-m, 24px);\n"
                    + "  border: 1px solid var(--color-border, #e2e8f0);\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-align-center {\n"
                    + "  text-align: center;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis {\n"
                    + "  background: var(--color-dark, #0f172a);\n"
                    + "  color: #ffffff;\n"
                    + "}\n";
        } else if ("content-migration".equalsIgnoreCase(agent)) {
            // Refine the REAL page markdown passed in the prompt — never replace it with
            // hardcoded sample content, otherwise generated pages stop matching their root path.
            String marker = "Refine migrated Markdown structure and tables:\n\n";
            String original = request.getPrompt() != null && request.getPrompt().contains(marker)
                    ? request.getPrompt().substring(request.getPrompt().indexOf(marker) + marker.length()).trim()
                    : "";
            if (!original.isEmpty()) {
                content = original; // mock provider passes the JCR-derived structure through unchanged
            } else {
                content = "# Migrated Page\n\n### Content\n| Text |\n| --- |\n| No source markdown received. |\n";
            }
        } else if ("visual-validation".equalsIgnoreCase(agent) || "advanced-visual-validation".equalsIgnoreCase(agent)) {
            content = "{\"visualScore\":0.98,\"a11yScore\":1.0,\"passed\":true,\"issues\":[]}";
        } else if ("self-repair".equalsIgnoreCase(agent) || "advanced-repair".equalsIgnoreCase(agent)) {
            content = "{\"successful\":true,\"patch\":\"/* verified CREATE_AEM_BLOCK.md standard */\",\"explanation\":\"Validated block structure against CREATE_AEM_BLOCK.md contract.\"}";
        } else if ("figma-intelligence".equalsIgnoreCase(agent) || "figma-analysis".equalsIgnoreCase(agent)) {
            content = "{\"tokens\":{\"--color-primary\":\"#f97316\",\"--font-heading\":\"'Plus Jakarta Sans', sans-serif\"},\"componentPairs\":[{\"figma\":\"Card/Adventure\",\"edsBlock\":\"cards\"}]}";
        } else if ("ai-page-comparison".equalsIgnoreCase(agent)) {
            content = "```css\n." + blockName + " {\n  margin: 24px auto;\n}\n```\n\n```js\n// page comparison refinement\n```";
        } else if ("pipeline-heal".equalsIgnoreCase(agent)) {
            String marker = "FILE:\n";
            String original = request.getPrompt() != null && request.getPrompt().contains(marker)
                    ? request.getPrompt().substring(request.getPrompt().indexOf(marker) + marker.length()).trim()
                    : "";
            content = original.isEmpty() ? "/* clean healed code */" : original;
        } else {
            content = "{\"status\":\"OK\",\"message\":\"Processed by AI Provider conforming to CREATE_AEM_BLOCK.md\",\"confidence\":0.96}";
        }

        ChatResponse response = new ChatResponse(content, providerName, chosenModel);
        response.setTokenUsage(new TokenUsage(185, 96));
        response.setCostUsd(0.0);
        response.setFinishReason("stop");
        return response;
    }
}
