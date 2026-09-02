package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
import com.adobe.aem.modernizer.ai.IdeAgentProviders;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobEventRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Generates EDS JavaScript block decoration logic (Stage: BUILDING).
 */
public class BlockGenerationAgent implements Agent {

    private static final Logger LOG = LoggerFactory.getLogger(BlockGenerationAgent.class);

    private final Store store;
    private final AiGateway ai;
    private final com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo;

    public BlockGenerationAgent(Store store, AiGateway ai) {
        this(store, ai, null);
    }

    public BlockGenerationAgent(Store store, AiGateway ai,
            com.adobe.aem.modernizer.connectors.LocalEdsRepoManager edsRepo) {
        this.store = store;
        this.ai = ai;
        this.edsRepo = edsRepo;
    }

    @Override
    public String getName() {
        return "block-generation";
    }

    @Override
    public MigrationState getStage() {
        return MigrationState.BUILDING;
    }

    /** Provider name constant that signals Antigravity IDE agent handles block generation. */
        @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("BlockGenerationAgent generating blocks for {} components", inv.getComponents().size());

        String aiProvider = ctx.getProject() != null ? ctx.getProject().getAiProvider() : null;
        boolean isIdeHandoff = IdeAgentProviders.isLocalOnlyProvider(aiProvider);

        if (isIdeHandoff && store != null) {
            String ideName = IdeAgentProviders.displayName(aiProvider);
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "✨ [" + ideName + "] IDE handoff — GET components-pending then POST blocks."
            ));
        }

        java.util.Set<String> existingEdsBlocks = BlockReconcileHelper.listExistingBlockNames(ctx, store);
        java.util.List<BlockReconcileHelper.Decision> decisions = new java.util.ArrayList<>();

        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            if (comp.getResourceType() != null && (comp.getResourceType().contains("/components/container")
                    || comp.getResourceType().contains("/components/page")
                    || comp.getResourceType().endsWith("/container")
                    || comp.getResourceType().endsWith("/page"))) {
                LOG.info("Skipping block creation for container/page component: {}", comp.getResourceType());
                continue;
            }
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock().toLowerCase().replace(' ', '-')
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1).toLowerCase().replace(' ', '-');

            BlockReconcileHelper.Action action = BlockReconcileHelper.decide(blockName, existingEdsBlocks, null);
            decisions.add(new BlockReconcileHelper.Decision(blockName, comp.getResourceType(), action));
            boolean isExistingBlock = (action == BlockReconcileHelper.Action.LEAVE);
            if (isExistingBlock) {
                LOG.info("Preserving existing implementation for block '{}' while ensuring documentation and fixtures", blockName);
            }

            String titleCase = Character.toUpperCase(blockName.charAt(0)) + blockName.substring(1).replace('-', ' ');

            // Dynamic JCR properties resolution
            java.util.Map<String, Object> jcrProps = new java.util.LinkedHashMap<>();
            String samplePagePath = null;
            for (SiteInventory.PageInfo pInfo : inv.getPages()) {
                if (pInfo.getComponentResourceTypes() != null && pInfo.getComponentResourceTypes().contains(comp.getResourceType())) {
                    samplePagePath = pInfo.getPath();
                    break;
                }
            }
            if (ctx.getProject() != null && samplePagePath != null) {
                jcrProps = fetchComponentProperties(ctx.getProject().getAemAuthorUrl(), samplePagePath, comp.getResourceType());
            }

            jcrProps.remove("id");
            jcrProps.remove("classes");

            // Authorable blockId / cssClass defaults for content-driven page styling
            String blockIdDefault = blockName + "-section-1";
            String cssClassDefault = "eds-block-" + blockName;

            java.util.List<String> propNames = new java.util.ArrayList<>(jcrProps.keySet());
            if (propNames.isEmpty()) {
                propNames.add("title");
                propNames.add("text");
                propNames.add("ctaLink");
                propNames.add("ctaContent");
                jcrProps.put("title", titleCase + " Experience");
                jcrProps.put("text", "Curated adventure and premium digital experiences delivered at lightning speed with Adobe Edge Delivery Services.");
                jcrProps.put("ctaLink", "/content/wknd/language-masters/en/adventures/ski-touring-mont-blanc");
                jcrProps.put("ctaContent", "Explore Stories");
            }

            // 1. <block-name>.js (Decoration logic + createBlock)
            StringBuilder jsBuilder = new StringBuilder();
            jsBuilder.append("import {\n")
                    .append("  checkAndHandleNestedBlocks,\n")
                    .append("  replaceBlockRowsPreservingNestedBlocks,\n")
                    .append("  getTextFromBlockRow,\n")
                    .append("  getHtmlFromRow,\n")
                    .append("  coerceAuthorClasses,\n")
                    .append("  escapeHtml,\n")
                    .append("  escapeHtmlAttribute,\n")
                    .append("  franklinBlockRow,\n")
                    .append("} from '../../scripts/utilities/block-helpers.js';\n\n")
                    .append("/**\n")
                    .append(" * Row layout parsed from JCR component schema:\n")
                    .append(" * 0: id\n");
            for (int i = 0; i < propNames.size(); i++) {
                jsBuilder.append(" * ").append(i + 1).append(": ").append(propNames.get(i)).append("\n");
            }
            jsBuilder.append(" */\n")
                    .append("function extractConfig(block) {\n")
                    .append("  if (!block) return {};\n")
                    .append("  const rows = [...block.children];\n")
                    .append("  return {\n")
                    .append("    id: getTextFromBlockRow(rows[0]),\n");
            for (int i = 0; i < propNames.size(); i++) {
                String pName = propNames.get(i);
                String jsName = jsSafeIdent(pName);
                if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    jsBuilder.append("    ").append(jsName).append(": rows[").append(i + 1).append("]?.querySelector('a') ? rows[").append(i + 1).append("].querySelector('a').getAttribute('href') : getTextFromBlockRow(rows[").append(i + 1).append("]),\n");
                } else {
                    jsBuilder.append("    ").append(jsName).append(": getHtmlFromRow(rows[").append(i + 1).append("]),\n");
                }
            }
            jsBuilder.append("  };\n")
                    .append("}\n\n")
                    .append("function buildBlock(block, config) {\n")
                    .append("  const inner = document.createElement('div');\n")
                    .append("  inner.classList.add('").append(blockName).append("-inner');\n\n");

            for (String pName : propNames) {
                String jsName = jsSafeIdent(pName);
                if (pName.toLowerCase().contains("image") || pName.toLowerCase().contains("file")) {
                    jsBuilder.append("  if (config.").append(jsName).append(") {\n")
                            .append("    const imgEl = document.createElement('img');\n")
                            .append("    imgEl.src = config.").append(jsName).append(";\n")
                            .append("    imgEl.alt = config.alt || '").append(titleCase).append("';\n")
                            .append("    imgEl.classList.add('").append(blockName).append("-image');\n")
                            .append("    inner.appendChild(imgEl);\n")
                            .append("  }\n\n");
                } else if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    jsBuilder.append("  if (config.").append(jsName).append(") {\n")
                            .append("    const linkEl = document.createElement('a');\n")
                            .append("    linkEl.href = config.").append(jsName).append(";\n")
                            .append("    linkEl.classList.add('brand-cta');\n")
                            .append("    linkEl.innerHTML = `<span>${config.ctaContent || 'Learn More'}</span>`;\n")
                            .append("    inner.appendChild(linkEl);\n")
                            .append("  }\n\n");
                } else {
                    jsBuilder.append("  if (config.").append(jsName).append(") {\n")
                            .append("    const divEl = document.createElement('div');\n")
                            .append("    divEl.classList.add('").append(blockName).append("-").append(jsName.toLowerCase()).append("');\n")
                            .append("    divEl.innerHTML = config.").append(jsName).append(";\n")
                            .append("    inner.appendChild(divEl);\n")
                            .append("  }\n\n");
                }
            }

            jsBuilder.append("  replaceBlockRowsPreservingNestedBlocks(block, inner);\n")
                    .append("  if (config.id) block.id = config.id;\n")
                    .append("  config.mainEl = inner;\n")
                    .append("}\n\n")
                    .append("function appendEvents() {\n")
                    .append("}\n\n")
                    .append("export default async function decorate(block) {\n")
                    .append("  await checkAndHandleNestedBlocks(block);\n")
                    .append("  const config = extractConfig(block);\n")
                    .append("  buildBlock(block, config);\n")
                    .append("  appendEvents(config);\n")
                    .append("}\n\n")
                    .append("export function createBlock(options = {}) {\n")
                    .append("  const id = escapeHtml(options.id ?? '');\n");
            for (String pName : propNames) {
                String jsName = jsSafeIdent(pName);
                jsBuilder.append("  const ").append(jsName).append(" = typeof options.").append(jsName).append(" === 'string' ? options.").append(jsName).append(" : '';\n");
            }
            jsBuilder.append("  const extra = coerceAuthorClasses(options.classes);\n")
                    .append("  const rootClasses = ['").append(blockName).append("', 'eds-block-").append(blockName).append("', extra].filter(Boolean).join(' ');\n")
                    .append("  return `<div class=\"${escapeHtmlAttribute(rootClasses)}\">${franklinBlockRow(id)}");
            for (String pName : propNames) {
                jsBuilder.append("${franklinBlockRow(").append(jsSafeIdent(pName)).append(")}");
            }
            jsBuilder.append("</div>`;\n")
                    .append("}\n");
            String jsContent = jsBuilder.toString();
            String existingJs = isExistingBlock ? readExistingLocalBlockFile(ctx, blockName, blockName + ".js") : null;
            if (existingJs != null && !existingJs.isBlank()) {
                jsContent = existingJs;
            }

            // 2. _<block-name>.json (Universal Editor Model)
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n")
                    .append("  \"definitions\": [\n")
                    .append("    {\n")
                    .append("      \"title\": \"").append(titleCase).append("\",\n")
                    .append("      \"id\": \"").append(blockName).append("\",\n")
                    .append("      \"plugins\": {\n")
                    .append("        \"xwalk\": {\n")
                    .append("          \"page\": {\n")
                    .append("            \"resourceType\": \"core/franklin/components/block/v1/block\",\n")
                    .append("            \"template\": {\n")
                    .append("              \"name\": \"").append(titleCase).append("\",\n")
                    .append("              \"model\": \"").append(blockName).append("\",\n")
                    .append("              \"filter\": \"").append(blockName).append("\",\n")
                    .append("              \"id\": \"").append(blockIdDefault).append("\",\n")
                    .append("              \"classes\": \"").append(cssClassDefault).append("\",\n");
            for (int i = 0; i < propNames.size(); i++) {
                String pName = propNames.get(i);
                Object pVal = jcrProps.get(pName);
                String strVal = pVal != null
                        ? com.adobe.aem.modernizer.connectors.PipelineHealRepairs.escapeJsonString(pVal.toString())
                        : "";
                jsonBuilder.append("              \"").append(pName).append("\": \"").append(strVal).append("\"");
                if (i < propNames.size() - 1) jsonBuilder.append(",");
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("            }\n")
                    .append("          }\n")
                    .append("        }\n")
                    .append("      }\n")
                    .append("    }\n")
                    .append("  ],\n")
                    .append("  \"models\": [\n")
                    .append("    {\n")
                    .append("      \"id\": \"").append(blockName).append("\",\n")
                    .append("      \"fields\": [\n")
                    .append("        { \"component\": \"tab\", \"label\": \"General\", \"name\": \"tabGeneral\" },\n")
                    .append("        { \"component\": \"text\", \"name\": \"id\", \"label\": \"Block ID (unique page anchor, e.g. wknd-about-us-hero)\", \"valueType\": \"string\", \"value\": \"").append(blockIdDefault).append("\" },\n")
                    .append("        { \"component\": \"multiselect\", \"name\": \"classes\", \"label\": \"CSS Classes (style variants)\", \"valueType\": \"string[]\", \"options\": [\n")
                    .append("          { \"name\": \"Dark tone\", \"value\": \"dark-tone\" },\n")
                    .append("          { \"name\": \"Compact spacing\", \"value\": \"compact\" },\n")
                    .append("          { \"name\": \"Emphasis tone\", \"value\": \"tone-emphasis\" },\n")
                    .append("          { \"name\": \"Align center\", \"value\": \"align-center\" },\n")
                    .append("          { \"name\": \"Align right\", \"value\": \"align-right\" }\n")
                    .append("        ], \"value\": [\"").append(cssClassDefault).append("\"] },\n");
            for (int i = 0; i < propNames.size(); i++) {
                String pName = propNames.get(i);
                String componentType = "text";
                if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    componentType = "aem-content";
                } else if (pName.toLowerCase().contains("text") || pName.toLowerCase().contains("desc") || pName.toLowerCase().contains("title")) {
                    componentType = "richtext";
                }
                String label = Character.toUpperCase(pName.charAt(0)) + pName.substring(1);
                jsonBuilder.append("        { \"component\": \"").append(componentType).append("\", \"name\": \"").append(pName).append("\", \"label\": \"").append(label).append("\" }");
                if (i < propNames.size() - 1) jsonBuilder.append(",");
                jsonBuilder.append("\n");
            }
            jsonBuilder.append("      ]\n")
                    .append("    }\n")
                    .append("  ],\n")
                    .append("  \"filters\": [\n")
                    .append("    { \"id\": \"").append(blockName).append("\", \"components\": [] }\n")
                    .append("  ]\n")
                    .append("}\n");

            String jsonContent = jsonBuilder.toString();

            // 5. <block-name>.css (Scoped Component Styles)
            String cssContent = "." + blockName + " {\n"
                    + "  margin: 28px auto;\n"
                    + "  max-width: 1200px;\n"
                    + "  padding: 0 16px;\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-inner {\n"
                    + "  background: #fff;\n"
                    + "  border-radius: 10px;\n"
                    + "  padding: 24px;\n"
                    + "  border: 1px solid #e2e8f0;\n"
                    + "  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 5%);\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-title h2 {\n"
                    + "  margin: 0 0 10px;\n"
                    + "  font-size: 1.6rem;\n"
                    + "  color: #0f172a;\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-text p {\n"
                    + "  color: #475569;\n"
                    + "  font-size: 0.95rem;\n"
                    + "  line-height: 1.6;\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-cta {\n"
                    + "  margin-top: 16px;\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-cta a.brand-cta {\n"
                    + "  display: inline-block;\n"
                    + "  background: #f97316;\n"
                    + "  color: #fff;\n"
                    + "  font-weight: 700;\n"
                    + "  padding: 10px 18px;\n"
                    + "  border-radius: 6px;\n"
                    + "  text-decoration: none;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-align-center {\n"
                    + "  text-align: center;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis ." + blockName + "-inner {\n"
                    + "  background: #0f172a;\n"
                    + "  color: #fff;\n"
                    + "  border-color: #334155;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis ." + blockName + "-title h2 {\n"
                    + "  color: #fff;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis ." + blockName + "-text p {\n"
                    + "  color: #cbd5e1;\n"
                    + "}\n";

            String existingCss = isExistingBlock ? readExistingLocalBlockFile(ctx, blockName, blockName + ".css") : null;
            if (existingCss != null && !existingCss.isBlank()) {
                cssContent = existingCss;
            }

            // 3. <block-name>-example.html (HTML Demo before & after decoration)
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<!doctype html>\n")
                    .append("<html lang=\"en\">\n")
                    .append("  <head>\n")
                    .append("    <meta charset=\"utf-8\" />\n")
                    .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n")
                    .append("    <title>").append(titleCase).append(" — Universal Editor Demo</title>\n")
                    .append("    <style>\n")
                    .append("      ").append(cssContent.replace("\n", "\n      ")).append("\n")
                    .append("    </style>\n")
                    .append("  </head>\n")
                    .append("  <body style=\"font-family:system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; padding:24px; background:#f8fafc;\">\n")
                    .append("    <main style=\"max-width:960px; margin:0 auto;\">\n")
                    .append("      <div class=\"block-example-variant-section\" style=\"margin-bottom:32px; background:#fff; padding:24px; border-radius:8px; border:1px solid #e2e8f0; box-shadow:0 1px 3px rgba(0,0,0,0.05);\">\n")
                    .append("        <h2 style=\"font-size:1.2rem; font-weight:700; margin-top:0; margin-bottom:4px; color:#0f172a;\">").append(titleCase).append(" Block</h2>\n")
                    .append("        <p style=\"color:#64748b; font-size:0.85rem; margin-top:0; margin-bottom:20px;\">JCR Path: <code>").append(samplePagePath != null ? samplePagePath : "contentRoot").append("</code></p>\n")
                    .append("        <div class=\"").append(blockName).append(" eds-block-").append(blockName).append("\">\n")
                    .append("          <div><div>demo-id</div></div>\n");
            for (String pName : propNames) {
                Object pVal = jcrProps.get(pName);
                String valStr = pVal != null ? pVal.toString() : "";
                if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    htmlBuilder.append("          <div><div><a href=\"").append(valStr).append("\">").append(valStr).append("</a></div></div>\n");
                } else {
                    htmlBuilder.append("          <div><div><p>").append(valStr).append("</p></div></div>\n");
                }
            }
            htmlBuilder.append("        </div>\n")
                    .append("      </div>\n")
                    .append("    </main>\n")
                    .append("    <script>\n")
                    .append("      (function() {\n")
                    .append("        function getText(row) {\n")
                    .append("          if (!row) return '';\n")
                    .append("          const col = row.querySelector('div');\n")
                    .append("          return col ? col.textContent.trim() : '';\n")
                    .append("        }\n")
                    .append("        function getHtml(row) {\n")
                    .append("          if (!row) return '';\n")
                    .append("          const col = row.querySelector('div');\n")
                    .append("          return col ? col.innerHTML : '';\n")
                    .append("        }\n")
                    .append("        const block = document.querySelector('.").append(blockName).append("');\n")
                    .append("        if (block) {\n")
                    .append("          const rows = [...block.children];\n")
                    .append("          const config = {\n")
                    .append("            id: getText(rows[0]),\n");
            for (int i = 0; i < propNames.size(); i++) {
                String pName = propNames.get(i);
                if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    htmlBuilder.append("            ").append(pName).append(": rows[").append(i + 1).append("]?.querySelector('a') ? rows[").append(i + 1).append("].querySelector('a').getAttribute('href') : getText(rows[").append(i + 1).append("]),\n");
                } else {
                    htmlBuilder.append("            ").append(pName).append(": getHtml(rows[").append(i + 1).append("]),\n");
                }
            }
            htmlBuilder.append("          };\n")
                    .append("          const inner = document.createElement('div');\n")
                    .append("          inner.className = '").append(blockName).append("-inner';\n");
            for (String pName : propNames) {
                if (pName.toLowerCase().contains("image") || pName.toLowerCase().contains("file")) {
                    htmlBuilder.append("          if (config.").append(pName).append(") {\n")
                            .append("            const img = document.createElement('img');\n")
                            .append("            img.src = config.").append(pName).append(";\n")
                            .append("            img.alt = '").append(titleCase).append("';\n")
                            .append("            img.className = '").append(blockName).append("-image';\n")
                            .append("            img.style.maxWidth = '100%';\n")
                            .append("            img.style.height = 'auto';\n")
                            .append("            inner.appendChild(img);\n")
                            .append("          }\n");
                } else if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    htmlBuilder.append("          if (config.").append(pName).append(") {\n")
                            .append("            const a = document.createElement('a');\n")
                            .append("            a.href = config.").append(pName).append(";\n")
                            .append("            a.className = 'brand-cta';\n")
                            .append("            a.style.display = 'inline-block';\n")
                            .append("            a.style.marginTop = '12px';\n")
                            .append("            a.style.padding = '8px 16px';\n")
                            .append("            a.style.background = '#f97316';\n")
                            .append("            a.style.color = '#fff';\n")
                            .append("            a.style.textDecoration = 'none';\n")
                            .append("            a.style.borderRadius = '4px';\n")
                            .append("            a.textContent = 'Explore';\n")
                            .append("            inner.appendChild(a);\n")
                            .append("          }\n");
                } else {
                    htmlBuilder.append("          if (config.").append(pName).append(") {\n")
                            .append("            const d = document.createElement('div');\n")
                            .append("            d.className = '").append(blockName).append("-").append(pName.toLowerCase()).append("';\n")
                            .append("            d.innerHTML = config.").append(pName).append(";\n")
                            .append("            inner.appendChild(d);\n")
                            .append("          }\n");
                }
            }
            htmlBuilder.append("          block.innerHTML = '';\n")
                    .append("          block.appendChild(inner);\n")
                    .append("          if (config.id) block.id = config.id;\n")
                    .append("        }\n")
                    .append("      })();\n")
                    .append("    </script>\n")
                    .append("  </body>\n")
                    .append("</html>\n");

            String htmlContent = htmlBuilder.toString();

            // 4. README.md (Documentation & LLM Selection)
            StringBuilder readmeBuilder = new StringBuilder();
            readmeBuilder.append("# ").append(titleCase).append(" Block (`").append(blockName).append("`)\n\n")
                    .append("## 1. Purpose\n")
                    .append("Renders the ").append(titleCase).append(" block with responsive layout and Universal Editor authoring.\n")
                    .append("Derived from AEM Component: `").append(comp.getResourceType()).append("`.\n\n")
                    .append("## 2. JCR Reference Content Path\n")
                    .append("- **Content Root:** `").append(ctx.getProject().getContentRoot()).append("`\n")
                    .append("- **Sample Page Path:** `").append(samplePagePath != null ? samplePagePath : "N/A").append("`\n\n")
                    .append("## 3. For another AI / LLM\n")
                    .append("- **Pick this block when:** The AEM component is `").append(comp.getResourceType()).append("`.\n")
                    .append("- **Do not pick when:** A simple text paragraph suffices.\n\n")
                    .append("## 4. Fields / options\n")
                    .append("| Field | Component | Row? | Description |\n")
                    .append("|---|---|---|---|\n")
                    .append("| `id` | text | Yes (row 0) | Authorable unique block ID (page anchor & AI target) |\n")
                    .append("| `classes` | multiselect | No | Authorable CSS variant classes (dark-tone, compact, ...) |\n");
            for (int i = 0; i < propNames.size(); i++) {
                String pName = propNames.get(i);
                String componentType = "text";
                if (pName.toLowerCase().contains("link") || pName.toLowerCase().contains("url") || pName.toLowerCase().contains("path")) {
                    componentType = "aem-content";
                } else if (pName.toLowerCase().contains("text") || pName.toLowerCase().contains("desc") || pName.toLowerCase().contains("title")) {
                    componentType = "richtext";
                }
                readmeBuilder.append("| `").append(pName).append("` | ").append(componentType).append(" | Yes (row ").append(i + 1).append(") | JCR property `").append(pName).append("` |\n");
            }
            readmeBuilder.append("\n## 5. Row Map\n")
                    .append("- **Row 0:** `id`\n");
            for (int i = 0; i < propNames.size(); i++) {
                readmeBuilder.append("- **Row ").append(i + 1).append(":** `").append(propNames.get(i)).append("`\n");
            }

            String readmeContent = readmeBuilder.toString();

            // Skip internal AI dispatch in Antigravity mode — Antigravity generates via MCP context
            if (ai != null && !isIdeHandoff) {
                String aiPrompt = "You are an expert AEM Edge Delivery Services (EDS) architect following docs/CREATE_AEM_BLOCK.md.\n\n"
                        + "### Architectural Contract (CREATE_AEM_BLOCK.md):\n"
                        + "1. Required deliverables: _<block>.json (UE model), <block>.js (decorate + createBlock), <block>.css (scoped CSS), <block>-example.html, README.md.\n"
                        + "2. No analytics imports (no dataLayer.js).\n"
                        + "3. JavaScript must use: import { checkAndHandleNestedBlocks, replaceBlockRowsPreservingNestedBlocks, getTextFromBlockRow, getHtmlFromRow, franklinBlockRow } from '../../scripts/utilities/block-helpers.js'.\n"
                        + "4. JavaScript must export `default async function decorate(block)` and named `export function createBlock(options)`.\n"
                        + "5. CSS must be scoped to `." + blockName + "` using design tokens.\n\n"
                        + "### Target AEM Component Details:\n"
                        + "- Component Name: " + titleCase + "\n"
                        + "- AEM ResourceType: " + comp.getResourceType() + "\n"
                        + "- Proposed EDS Block: " + blockName + "\n"
                        + "- Variants: default, emphasis\n\n"
                        + "### Task:\n"
                        + "Build the complete Edge Delivery Services JavaScript decoration code `decorate(block)` and `createBlock(options)` for the `" + blockName + "` component.";

                if (ai != null && (!isExistingBlock || existingJs == null) && (ctx.getProject() != null && ctx.getProject().getAiProvider() != null && !ctx.getProject().getAiProvider().equalsIgnoreCase("mock"))) {
                    ChatRequest req = new ChatRequest(getName(), aiPrompt);
                    req.setTargetCapability(ModelCapability.CAP_CODE);
                    req.setPreferredProvider(ctx.getProject().getAiProvider());
                    req.setPreferredModel(ctx.getProject().getAiModel());
                    req.setProjectId(ctx.getProject().getId());
                    req.setJobId(ctx.getJob().getId());
                    try {
                        ChatResponse resp = ai.dispatch(req);
                        if (resp.getContent() != null && (resp.getContent().contains("decorate") || resp.getContent().contains("export default"))) {
                            jsContent = resp.getContent();
                        }
                    } catch (Exception e) {
                        LOG.debug("AI generation fallback for block {}: {}", blockName, e.getMessage());
                    }
                }
            }

            GeneratedFileRecord jsFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + ".js", "BLOCK_JS", jsContent);
            GeneratedFileRecord cssFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + ".css", "BLOCK_CSS", cssContent);
            GeneratedFileRecord jsonFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/_" + blockName + ".json", "BLOCK_MODEL_JSON", jsonContent);
            GeneratedFileRecord htmlFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + "-example.html", "BLOCK_EXAMPLE_HTML", htmlContent);
            GeneratedFileRecord readmeFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/README.md", "BLOCK_README", readmeContent);

            // Reference the AEM root path this block was authored from, so blocks and pages
            // share the same JCR source reference (keeps page scope and block content in sync)
            String blockSourcePath = samplePagePath != null
                    ? samplePagePath
                    : (ctx.getProject() != null ? ctx.getProject().getContentRoot() : null);
            jsFile.setSourcePath(blockSourcePath);
            cssFile.setSourcePath(blockSourcePath);
            jsonFile.setSourcePath(blockSourcePath);
            htmlFile.setSourcePath(blockSourcePath);
            readmeFile.setSourcePath(blockSourcePath);

            jsFile.setVirtualDiffOnly(ctx.isDryRun());
            cssFile.setVirtualDiffOnly(ctx.isDryRun());
            jsonFile.setVirtualDiffOnly(ctx.isDryRun());
            htmlFile.setVirtualDiffOnly(ctx.isDryRun());
            readmeFile.setVirtualDiffOnly(ctx.isDryRun());

            if (store != null) {
                store.saveGeneratedFile(jsFile);
                store.saveGeneratedFile(cssFile);
                store.saveGeneratedFile(jsonFile);
                store.saveGeneratedFile(htmlFile);
                store.saveGeneratedFile(readmeFile);
            }

            if (!ctx.isDryRun()) {
                // Direct output into the cloned EDS repo: eds/<projectId>/blocks/<blockName>/
                String projectId = ctx.getProject().getId();
                String base = "blocks/" + blockName;
                if (edsRepo != null) {
                    if (!isExistingBlock || existingJs == null) {
                        edsRepo.writeProjectFile(projectId, base + "/" + blockName + ".js", jsContent);
                    }
                    if (!isExistingBlock || existingCss == null) {
                        edsRepo.writeProjectFile(projectId, base + "/" + blockName + ".css", cssContent);
                    }
                    edsRepo.writeProjectFile(projectId, base + "/_" + blockName + ".json", jsonContent);
                    edsRepo.writeProjectFile(projectId, base + "/" + blockName + "-example.html", htmlContent);
                    edsRepo.writeProjectFile(projectId, base + "/README.md", readmeContent);
                } else {
                    if (!isExistingBlock || existingJs == null) {
                        writeLocalFile(ctx, base + "/" + blockName + ".js", jsContent);
                    }
                    if (!isExistingBlock || existingCss == null) {
                        writeLocalFile(ctx, base + "/" + blockName + ".css", cssContent);
                    }
                    writeLocalFile(ctx, base + "/_" + blockName + ".json", jsonContent);
                    writeLocalFile(ctx, base + "/" + blockName + "-example.html", htmlContent);
                    writeLocalFile(ctx, base + "/README.md", readmeContent);
                }
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    BlockReconcileHelper.summarize(decisions)
            ));
        }
    }

    private String readExistingLocalBlockFile(AgentContext ctx, String blockName, String fileName) {
        if (ctx == null || ctx.getProject() == null) return null;
        String projectId = ctx.getProject().getId();
        java.io.File repoDir = (edsRepo != null) ? edsRepo.edsRepoDir(projectId) : new java.io.File("D:/eds personal/AEM-EDS-Modernizer/eds", projectId);
        java.io.File target = new java.io.File(repoDir, "blocks/" + blockName + "/" + fileName);
        if (target.isFile()) {
            try {
                return java.nio.file.Files.readString(target.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOG.debug("Could not read local block file {}: {}", target.getAbsolutePath(), e.getMessage());
            }
        }
        return null;
    }

    private void loadExistingLocalBlockFiles(AgentContext ctx, String blockName) {
        if (store == null || ctx == null || ctx.getProject() == null || ctx.getJob() == null) return;
        String projectId = ctx.getProject().getId();
        java.io.File repoDir = (edsRepo != null) ? edsRepo.edsRepoDir(projectId) : new java.io.File("D:/eds personal/AEM-EDS-Modernizer/eds", projectId);
        java.io.File blockDir = new java.io.File(repoDir, "blocks/" + blockName);
        if (blockDir.isDirectory()) {
            java.io.File[] files = blockDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isFile()) {
                        try {
                            String content = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                            String relPath = "blocks/" + blockName + "/" + f.getName();
                            String fileType = f.getName().endsWith(".js") ? "BLOCK_JS"
                                    : f.getName().endsWith(".css") ? "BLOCK_CSS"
                                    : f.getName().endsWith(".json") ? "BLOCK_MODEL_JSON"
                                    : f.getName().endsWith(".html") ? "BLOCK_EXAMPLE_HTML"
                                    : "BLOCK_README";
                            GeneratedFileRecord record = new GeneratedFileRecord(
                                    UUID.randomUUID().toString(),
                                    ctx.getProject().getId(),
                                    ctx.getJob().getId(),
                                    relPath,
                                    fileType,
                                    content
                            );
                            record.setVirtualDiffOnly(ctx.isDryRun());
                            store.saveGeneratedFile(record);
                        } catch (Exception e) {
                            LOG.warn("Could not read local block file {}: {}", f.getAbsolutePath(), e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void writeLocalFile(AgentContext ctx, String relPath, String content) {
        String projectId = (ctx != null && ctx.getProject() != null) ? ctx.getProject().getId() : "project";
        java.io.File target = new java.io.File(new java.io.File("D:/eds personal/AEM-EDS-Modernizer/eds", projectId), relPath);
        try {
            target.getParentFile().mkdirs();
            java.nio.file.Files.writeString(target.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            LOG.info("Wrote local block file in workspace: {}", target.getAbsolutePath());
        } catch (Exception e) {
            LOG.warn("Could not write local block file {}: {}", relPath, e.getMessage());
        }
    }

    private java.util.Map<String, Object> fetchComponentProperties(String authorUrl, String samplePagePath, String resourceType) {
        java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
        if (authorUrl == null || samplePagePath == null || resourceType == null) return props;
        try {
            String url = authorUrl + (samplePagePath.startsWith("/") ? samplePagePath : ("/" + samplePagePath)) + ".infinity.json";
            String credentials = "admin:admin";
            String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", authHeader)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().trim().startsWith("{")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(resp.body());
                java.util.List<com.fasterxml.jackson.databind.JsonNode> matches = new java.util.ArrayList<>();
                collectComponentNodes(rootNode, resourceType, matches);

                for (com.fasterxml.jackson.databind.JsonNode compNode : matches) {
                    java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = compNode.fields();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                        String name = field.getKey();
                        com.fasterxml.jackson.databind.JsonNode val = field.getValue();
                        if (name.equals("jcr:primaryType") || name.equals("jcr:createdBy") || name.equals("jcr:created") || name.equals("jcr:mixinTypes") || name.equals("jcr:lastModified") || name.equals("jcr:lastModifiedBy") || name.startsWith("sling:") || name.startsWith("cq:") || name.startsWith("oak:")) {
                            continue;
                        }
                        if (val.isValueNode() && !val.asText().trim().isEmpty()) {
                            props.put(name, val.asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to fetch component properties from {}: {}", samplePagePath, e.getMessage());
        }

        if (props.isEmpty()) {
            if (resourceType.toLowerCase().contains("title")) {
                props.put("jcr:title", "Title");
            } else if (resourceType.toLowerCase().contains("image") || resourceType.toLowerCase().contains("media")) {
                props.put("fileReference", "/content/dam/wknd/default.jpg");
            } else {
                props.put("text", "Default Content");
            }
        }
        return props;
    }

    /** Turns JCR names such as {@code jcr:title} into valid JavaScript identifiers. */
    static String jsSafeIdent(String name) {
        if (name == null || name.isEmpty()) {
            return "prop";
        }
        StringBuilder out = new StringBuilder();
        boolean capNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ':' || c == '-' || c == '.' || c == '/') {
                capNext = true;
                continue;
            }
            if (out.length() == 0 && !Character.isJavaIdentifierStart(c)) {
                out.append('p');
            }
            if (capNext) {
                out.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                out.append(c);
            }
        }
        return out.length() == 0 ? "prop" : out.toString();
    }

    private void collectComponentNodes(com.fasterxml.jackson.databind.JsonNode node, String resourceType, java.util.List<com.fasterxml.jackson.databind.JsonNode> matches) {
        if (node == null) return;
        if (node.isObject()) {
            if (node.has("sling:resourceType") && resourceType.equals(node.get("sling:resourceType").asText())) {
                matches.add(node);
            }
            java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectComponentNodes(elements.next(), resourceType, matches);
            }
        } else if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode n : node) {
                collectComponentNodes(n, resourceType, matches);
            }
        }
    }
}
