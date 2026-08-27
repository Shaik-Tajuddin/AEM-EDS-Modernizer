package com.adobe.aem.modernizer.agents;

import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ChatResponse;
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

    public BlockGenerationAgent(Store store, AiGateway ai) {
        this.store = store;
        this.ai = ai;
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
    private static final String PROVIDER_ANTIGRAVITY = "antigravity";

    @Override
    public void execute(AgentContext ctx) throws com.adobe.aem.modernizer.ModernizerException {
        SiteInventory inv = ctx.getInventory();
        if (inv == null || inv.getComponents() == null) return;

        LOG.info("BlockGenerationAgent generating blocks for {} components", inv.getComponents().size());

        // ─── Antigravity Mode ────────────────────────────────────────────────────
        // When aiProvider = "antigravity", we skip internal AI dispatch entirely.
        // Instead we emit a pending-components event so the Antigravity IDE agent
        // can call GET /components-pending, enrich via MCP, generate the files,
        // and POST them back via POST /blocks.
        // The pipeline continues with hardcoded scaffold templates so the job
        // doesn't stall — Antigravity files will overwrite them when POSTed back.
        boolean isAntigravity = ctx.getProject() != null
                && PROVIDER_ANTIGRAVITY.equalsIgnoreCase(ctx.getProject().getAiProvider());

        if (isAntigravity && store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "✨ [Antigravity] " + inv.getComponents().size() + " components ready for block generation.\n"
                    + "→ Call: GET /bin/aem-eds-modernizer/api?path=projects/" + ctx.getProject().getId() + "/components-pending\n"
                    + "→ Antigravity will fetch real JCR dialog fields via MCP, generate block files,\n"
                    + "  and POST them back to: POST /bin/aem-eds-modernizer/api?path=projects/" + ctx.getProject().getId() + "/blocks\n"
                    + "Scaffolding templates are being saved now as placeholders."
            ));
            LOG.info("[Antigravity] mode active — scaffold templates will be saved as placeholders for {} components.",
                    inv.getComponents().size());
        }
        // ────────────────────────────────────────────────────────────────────────
        for (SiteInventory.ComponentInfo comp : inv.getComponents()) {
            String blockName = comp.getProposedEdsBlock() != null
                    ? comp.getProposedEdsBlock().toLowerCase().replace(' ', '-')
                    : comp.getResourceType().substring(comp.getResourceType().lastIndexOf('/') + 1).toLowerCase().replace(' ', '-');
            String titleCase = Character.toUpperCase(blockName.charAt(0)) + blockName.substring(1).replace('-', ' ');

            // 1. <block-name>.js (Decoration logic + createBlock)
            String jsContent = "import {\n"
                    + "  checkAndHandleNestedBlocks,\n"
                    + "  replaceBlockRowsPreservingNestedBlocks,\n"
                    + "  getTextFromBlockRow,\n"
                    + "  getHtmlFromBlockRow,\n"
                    + "  coerceAuthorClasses,\n"
                    + "  escapeHtml,\n"
                    + "  escapeHtmlAttribute,\n"
                    + "  franklinBlockRow,\n"
                    + "} from '../../scripts/utilities/block-helpers.js';\n\n"
                    + "/**\n"
                    + " * Row layout — tab / classes / classes_* create no rows.\n"
                    + " * 0: id\n"
                    + " * 1: title (richtext)\n"
                    + " * 2: text (richtext)\n"
                    + " * 3: ctaLink (aem-content)\n"
                    + " * 4: ctaContent (richtext)\n"
                    + " */\n"
                    + "function extractConfig(block) {\n"
                    + "  if (!block) return {};\n"
                    + "  const rows = [...block.children];\n"
                    + "  const anchor = rows[3]?.querySelector('a');\n"
                    + "  return {\n"
                    + "    id: getTextFromBlockRow(rows[0]),\n"
                    + "    title: getHtmlFromBlockRow(rows[1]),\n"
                    + "    text: getHtmlFromBlockRow(rows[2]),\n"
                    + "    ctaLink: anchor instanceof HTMLAnchorElement ? anchor.getAttribute('href') : '',\n"
                    + "    ctaContent: getHtmlFromBlockRow(rows[4]),\n"
                    + "  };\n"
                    + "}\n\n"
                    + "function buildBlock(block, config) {\n"
                    + "  const inner = document.createElement('div');\n"
                    + "  inner.classList.add('" + blockName + "-inner');\n\n"
                    + "  if (config.title) {\n"
                    + "    const titleEl = document.createElement('div');\n"
                    + "    titleEl.classList.add('" + blockName + "-title');\n"
                    + "    titleEl.innerHTML = config.title;\n"
                    + "    inner.appendChild(titleEl);\n"
                    + "  }\n\n"
                    + "  if (config.text) {\n"
                    + "    const textEl = document.createElement('div');\n"
                    + "    textEl.classList.add('" + blockName + "-text');\n"
                    + "    textEl.innerHTML = config.text;\n"
                    + "    inner.appendChild(textEl);\n"
                    + "  }\n\n"
                    + "  if (config.ctaLink || config.ctaContent) {\n"
                    + "    const ctaWrap = document.createElement('div');\n"
                    + "    ctaWrap.classList.add('" + blockName + "-cta');\n"
                    + "    if (config.ctaLink) {\n"
                    + "      const a = document.createElement('a');\n"
                    + "      a.href = config.ctaLink;\n"
                    + "      a.classList.add('brand-cta');\n"
                    + "      a.innerHTML = config.ctaContent || '<span>Learn more</span>';\n"
                    + "      ctaWrap.appendChild(a);\n"
                    + "    } else {\n"
                    + "      ctaWrap.innerHTML = config.ctaContent;\n"
                    + "    }\n"
                    + "    inner.appendChild(ctaWrap);\n"
                    + "  }\n\n"
                    + "  replaceBlockRowsPreservingNestedBlocks(block, inner);\n"
                    + "  if (config.id) block.id = config.id;\n"
                    + "  // eslint-disable-next-line no-param-reassign\n"
                    + "  config.mainEl = inner;\n"
                    + "}\n\n"
                    + "function appendEvents(config) {\n"
                    + "  if (!config?.mainEl) return;\n"
                    + "}\n\n"
                    + "export default async function decorate(block) {\n"
                    + "  await checkAndHandleNestedBlocks(block);\n"
                    + "  const config = extractConfig(block);\n"
                    + "  buildBlock(block, config);\n"
                    + "  appendEvents(config);\n"
                    + "}\n\n"
                    + "export function createBlock(options = {}) {\n"
                    + "  const id = escapeHtml(options.id ?? '');\n"
                    + "  const title = typeof options.title === 'string' ? options.title : '';\n"
                    + "  const text = typeof options.text === 'string' ? options.text : '';\n"
                    + "  const ctaLink = escapeHtmlAttribute(options.ctaLink ?? '');\n"
                    + "  const ctaContent = typeof options.ctaContent === 'string' && options.ctaContent.trim()\n"
                    + "    ? options.ctaContent : '<p>Learn more</p>';\n"
                    + "  const extra = coerceAuthorClasses(options.classes);\n"
                    + "  const rootClasses = ['" + blockName + "', 'eds-block-" + blockName + "', extra].filter(Boolean).join(' ');\n"
                    + "  const ctaRow = ctaLink\n"
                    + "    ? `${franklinBlockRow(`<a href=\"${ctaLink}\">Learn more</a>`)}${franklinBlockRow(ctaContent)}`\n"
                    + "    : '';\n"
                    + "  return `<div class=\"${escapeHtmlAttribute(rootClasses)}\">${franklinBlockRow(id)}${franklinBlockRow(\n"
                    + "    title\n"
                    + "  )}${franklinBlockRow(text)}${ctaRow}</div>`;\n"
                    + "}\n";

            // 2. _<block-name>.json (Universal Editor Model)
            String jsonContent = "{\n"
                    + "  \"definitions\": [\n"
                    + "    {\n"
                    + "      \"title\": \"" + titleCase + "\",\n"
                    + "      \"id\": \"" + blockName + "\",\n"
                    + "      \"plugins\": {\n"
                    + "        \"xwalk\": {\n"
                    + "          \"page\": {\n"
                    + "            \"resourceType\": \"core/franklin/components/block/v1/block\",\n"
                    + "            \"template\": {\n"
                    + "              \"name\": \"" + titleCase + "\",\n"
                    + "              \"model\": \"" + blockName + "\",\n"
                    + "              \"filter\": \"" + blockName + "\",\n"
                    + "              \"id\": \"\",\n"
                    + "              \"classes\": \"eds-block-" + blockName + "\",\n"
                    + "              \"title\": \"<p>Lorem ipsum dolor sit amet.</p>\",\n"
                    + "              \"text\": \"<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>\",\n"
                    + "              \"classes_align\": \"\",\n"
                    + "              \"classes_tone\": \"" + blockName + "-tone-default\"\n"
                    + "            }\n"
                    + "          }\n"
                    + "        }\n"
                    + "      }\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"models\": [\n"
                    + "    {\n"
                    + "      \"id\": \"" + blockName + "\",\n"
                    + "      \"fields\": [\n"
                    + "        { \"component\": \"tab\", \"label\": \"General\", \"name\": \"tabGeneral\" },\n"
                    + "        { \"component\": \"text\", \"name\": \"id\", \"label\": \"Block ID\", \"valueType\": \"string\", \"value\": \"\" },\n"
                    + "        { \"component\": \"text\", \"name\": \"classes\", \"label\": \"Block classes\", \"valueType\": \"string\", \"value\": \"eds-block-" + blockName + "\", \"hidden\": true, \"readOnly\": true },\n"
                    + "        { \"component\": \"richtext\", \"name\": \"title\", \"label\": \"Title\" },\n"
                    + "        { \"component\": \"richtext\", \"name\": \"text\", \"label\": \"Text\" },\n"
                    + "        { \"component\": \"aem-content\", \"name\": \"ctaLink\", \"label\": \"CTA link\" },\n"
                    + "        { \"component\": \"richtext\", \"name\": \"ctaContent\", \"label\": \"CTA text\" },\n"
                    + "        { \"component\": \"tab\", \"label\": \"Appearance\", \"name\": \"tabAppearance\" },\n"
                    + "        {\n"
                    + "          \"component\": \"select\",\n"
                    + "          \"name\": \"classes_align\",\n"
                    + "          \"label\": \"Alignment\",\n"
                    + "          \"valueType\": \"string\",\n"
                    + "          \"value\": \"\",\n"
                    + "          \"options\": [\n"
                    + "            { \"name\": \"Left\", \"value\": \"\" },\n"
                    + "            { \"name\": \"Center\", \"value\": \"" + blockName + "-align-center\" },\n"
                    + "            { \"name\": \"Right\", \"value\": \"" + blockName + "-align-right\" }\n"
                    + "          ]\n"
                    + "        },\n"
                    + "        {\n"
                    + "          \"component\": \"select\",\n"
                    + "          \"name\": \"classes_tone\",\n"
                    + "          \"label\": \"Tone\",\n"
                    + "          \"valueType\": \"string\",\n"
                    + "          \"value\": \"" + blockName + "-tone-default\",\n"
                    + "          \"options\": [\n"
                    + "            { \"name\": \"Default\", \"value\": \"" + blockName + "-tone-default\" },\n"
                    + "            { \"name\": \"Emphasis\", \"value\": \"" + blockName + "-tone-emphasis\" }\n"
                    + "          ]\n"
                    + "        }\n"
                    + "      ]\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"filters\": [\n"
                    + "    { \"id\": \"" + blockName + "\", \"components\": [] }\n"
                    + "  ]\n"
                    + "}\n";

            // 3. <block-name>-example.html (HTML Demo before & after decoration)
            String htmlContent = "<!doctype html>\n"
                    + "<html lang=\"en\">\n"
                    + "  <head>\n"
                    + "    <meta charset=\"utf-8\" />\n"
                    + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n"
                    + "    <title>" + titleCase + " — Universal Editor Demo</title>\n"
                    + "    <link rel=\"stylesheet\" href=\"/styles/styles.css\" />\n"
                    + "    <link rel=\"stylesheet\" href=\"" + blockName + ".css\" />\n"
                    + "  </head>\n"
                    + "  <body style=\"font-family:system-ui, sans-serif; padding:24px; background:#f8fafc;\">\n"
                    + "    <!--\n"
                    + "      " + blockName + "-example.html\n"
                    + "      Shows AEM Universal Editor generated markup before & after decorate() runs.\n"
                    + "    -->\n"
                    + "    <main style=\"max-width:960px; margin:0 auto;\">\n"
                    + "      <div class=\"block-example-variant-section\" style=\"margin-bottom:32px; background:#fff; padding:24px; border-radius:8px; border:1px solid #e2e8f0;\">\n"
                    + "        <h2 style=\"font-size:1.25rem; font-weight:700; margin-bottom:8px;\">Default Variation</h2>\n"
                    + "        <p style=\"color:#64748b; font-size:0.9rem; margin-bottom:16px;\"><code>classes_tone</code> = <code>" + blockName + "-tone-default</code>, Left aligned.</p>\n"
                    + "        <div class=\"" + blockName + " eds-block-" + blockName + " " + blockName + "-tone-default\">\n"
                    + "          <div><div>demo-default</div></div>\n"
                    + "          <div><div><p>" + titleCase + " Headline</p></div></div>\n"
                    + "          <div><div><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio. Praesent libero.</p></div></div>\n"
                    + "          <div><div><a href=\"#\">Learn More</a></div></div>\n"
                    + "          <div><div><p>Learn More</p></div></div>\n"
                    + "        </div>\n"
                    + "      </div>\n\n"
                    + "      <div class=\"block-example-variant-section\" style=\"background:#fff; padding:24px; border-radius:8px; border:1px solid #e2e8f0;\">\n"
                    + "        <h2 style=\"font-size:1.25rem; font-weight:700; margin-bottom:8px;\">Center Aligned, Emphasis Tone</h2>\n"
                    + "        <p style=\"color:#64748b; font-size:0.9rem; margin-bottom:16px;\"><code>classes_align</code> = <code>" + blockName + "-align-center</code>, <code>classes_tone</code> = <code>" + blockName + "-tone-emphasis</code>.</p>\n"
                    + "        <div class=\"" + blockName + " eds-block-" + blockName + " " + blockName + "-align-center " + blockName + "-tone-emphasis\">\n"
                    + "          <div><div>demo-emphasis</div></div>\n"
                    + "          <div><div><p>Promoted Feature</p></div></div>\n"
                    + "          <div><div><p>Highlight your most important content with bold styling and prominent call-to-action buttons.</p></div></div>\n"
                    + "          <div><div><a href=\"#\">Get Started</a></div></div>\n"
                    + "          <div><div><p>Get Started</p></div></div>\n"
                    + "        </div>\n"
                    + "      </div>\n"
                    + "    </main>\n"
                    + "  </body>\n"
                    + "</html>\n";

            // 4. README.md (Documentation & LLM Selection)
            String readmeContent = "# " + titleCase + " Block (`" + blockName + "`)\n\n"
                    + "## 1. Purpose\n"
                    + "Renders the " + titleCase + " block with responsive layout and Universal Editor authoring.\n\n"
                    + "## 2. For another AI / LLM\n"
                    + "- **Pick this block when:** The AEM component is `" + comp.getResourceType() + "`.\n"
                    + "- **Do not pick when:** A simple text paragraph suffices.\n\n"
                    + "## 3. Fields / options\n"
                    + "| Field | Component | Row? | Description |\n"
                    + "|---|---|---|---|\n"
                    + "| `id` | text | Yes (row 0) | Optional HTML anchor ID |\n"
                    + "| `classes` | text (hidden) | No | Root class `eds-block-" + blockName + "` |\n"
                    + "| `title` | richtext | Yes (row 1) | Block heading |\n"
                    + "| `text` | richtext | Yes (row 2) | Main body text |\n"
                    + "| `ctaLink` | aem-content | Yes (row 3) | Target URL |\n"
                    + "| `ctaContent` | richtext | Yes (row 4) | Button text |\n"
                    + "| `classes_align` | select | No | Alignment variant (Left, Center, Right) |\n"
                    + "| `classes_tone` | select | No | Visual tone (Default, Emphasis) |\n\n"
                    + "## 4. Row Map\n"
                    + "- **Row 0:** `id`\n"
                    + "- **Row 1:** `title`\n"
                    + "- **Row 2:** `text`\n"
                    + "- **Row 3:** `ctaLink`\n"
                    + "- **Row 4:** `ctaContent`\n";

            // 5. <block-name>.css (Scoped Component Styles)
            String cssContent = "." + blockName + " {\n"
                    + "  margin: 28px auto;\n"
                    + "  max-width: 1200px;\n"
                    + "  padding: 0 16px;\n"
                    + "}\n\n"
                    + "." + blockName + " ." + blockName + "-inner {\n"
                    + "  background: #ffffff;\n"
                    + "  border-radius: 10px;\n"
                    + "  padding: 24px;\n"
                    + "  border: 1px solid #e2e8f0;\n"
                    + "  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);\n"
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
                    + "  color: #ffffff;\n"
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
                    + "  color: #ffffff;\n"
                    + "  border-color: #334155;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis ." + blockName + "-title h2 {\n"
                    + "  color: #ffffff;\n"
                    + "}\n\n"
                    + "." + blockName + "." + blockName + "-tone-emphasis ." + blockName + "-text p {\n"
                    + "  color: #cbd5e1;\n"
                    + "}\n";

            // Skip internal AI dispatch in Antigravity mode — Antigravity generates via MCP context
            if (ai != null && !isAntigravity) {
                String aiPrompt = "You are an expert AEM Edge Delivery Services (EDS) architect following docs/CREATE_AEM_BLOCK.md.\n\n"
                        + "### Architectural Contract (CREATE_AEM_BLOCK.md):\n"
                        + "1. Required deliverables: _<block>.json (UE model), <block>.js (decorate + createBlock), <block>.css (scoped CSS), <block>-example.html, README.md.\n"
                        + "2. No analytics imports (no dataLayer.js).\n"
                        + "3. JavaScript must use: import { checkAndHandleNestedBlocks, replaceBlockRowsPreservingNestedBlocks, getTextFromBlockRow, getHtmlFromBlockRow, franklinBlockRow } from '../../scripts/utilities/block-helpers.js'.\n"
                        + "4. JavaScript must export `default async function decorate(block)` and named `export function createBlock(options)`.\n"
                        + "5. CSS must be scoped to `." + blockName + "` using design tokens.\n\n"
                        + "### Target AEM Component Details:\n"
                        + "- Component Name: " + titleCase + "\n"
                        + "- AEM ResourceType: " + comp.getResourceType() + "\n"
                        + "- Proposed EDS Block: " + blockName + "\n"
                        + "- Variants: default, emphasis\n\n"
                        + "### Task:\n"
                        + "Build the complete Edge Delivery Services JavaScript decoration code `decorate(block)` and `createBlock(options)` for the `" + blockName + "` component.";

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

            GeneratedFileRecord jsFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + ".js", "BLOCK_JS", jsContent);
            GeneratedFileRecord cssFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + ".css", "BLOCK_CSS", cssContent);
            GeneratedFileRecord jsonFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/_" + blockName + ".json", "BLOCK_MODEL_JSON", jsonContent);
            GeneratedFileRecord htmlFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/" + blockName + "-example.html", "BLOCK_EXAMPLE_HTML", htmlContent);
            GeneratedFileRecord readmeFile = new GeneratedFileRecord(UUID.randomUUID().toString(), ctx.getProject().getId(), ctx.getJob().getId(), "blocks/" + blockName + "/README.md", "BLOCK_README", readmeContent);

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
                writeLocalFile("blocks/" + blockName + "/" + blockName + ".js", jsContent);
                writeLocalFile("blocks/" + blockName + "/" + blockName + ".css", cssContent);
                writeLocalFile("blocks/" + blockName + "/_" + blockName + ".json", jsonContent);
                writeLocalFile("blocks/" + blockName + "/" + blockName + "-example.html", htmlContent);
                writeLocalFile("blocks/" + blockName + "/README.md", readmeContent);
            }
        }

        if (store != null) {
            store.recordEvent(new JobEventRecord(
                    UUID.randomUUID().toString(),
                    ctx.getProject().getId(),
                    ctx.getJob().getId(),
                    getName(),
                    "Generated full Block Quad (JS, JSON Model, Example HTML, README) for " + inv.getComponents().size() + " EDS blocks."
            ));
        }
    }

    private void writeLocalFile(String relPath, String content) {
        try {
            java.io.File target = null;
            String[] candidateRoots = new String[] {
                "D:/eds personal/AEM-EDS-Modernizer",
                "d:/eds personal/AEM-EDS-Modernizer",
                System.getProperty("user.dir")
            };
            for (String root : candidateRoots) {
                java.io.File dir = new java.io.File(root);
                if (new java.io.File(dir, "pom.xml").exists() || new java.io.File(dir, "blocks").exists()) {
                    target = new java.io.File(dir, relPath);
                    break;
                }
            }
            if (target == null) {
                target = new java.io.File("D:/eds personal/AEM-EDS-Modernizer", relPath);
            }
            java.io.File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.writeString(target.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            LOG.info("Wrote local block file: {}", target.getAbsolutePath());
        } catch (Exception e) {
            LOG.warn("Could not write local block file {}: {}", relPath, e.getMessage());
        }
    }
}
