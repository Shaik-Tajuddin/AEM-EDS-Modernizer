# Repository Analysis: AEM-EDS-Modernizer

## 1. Executive Summary & Repository Identity

- **Project Name:** AEM-EDS-Modernizer (`aem-eds-modernizer`)
- **Type:** Multi-module Apache Maven AEM Cloud Service / Local SDK Project + Edge Delivery Services (EDS) Hybrid Workspace.
- **Java / Build Baseline:** Java 11 (`maven.compiler.source=11`, `target=11`, `release=11`), Apache Maven 3.x, Apache Felix / Bnd OSGi Bundle Plugin, Jackrabbit FileVault Package Maven Plugin.
- **AEM Local SDK Environment:**
  - **AEM Author:** `http://localhost:4502`
  - **AEM Publish:** `http://localhost:4503`
  - **Default Credentials:** `admin:admin`
  - **Target Deployment Namespace:** `/apps/aem-eds-modernizer`, `/var/aem-eds-modernizer`, `/content/aem-eds-modernizer`

---

## 2. Repository Structure & Module Breakdown

### 2.1 Maven Multi-Module Hierarchy (`pom.xml`)
```text
.
├── pom.xml (Reactor parent POM)
├── all/ (Container package combining OSGi bundle, ui.apps, ui.config, ui.content)
├── core/ (OSGi bundle & standalone JAR: Java agents, AI gateway, persistence, REST router, SSRF security)
├── ui.apps/ (AEM application components, clientlibs: JS/CSS dashboard, HTL scripts)
├── ui.config/ (OSGi configuration factories, RepoInit scripts, service user definitions)
├── ui.content/ (Mutable content package: /content/aem-eds-modernizer)
├── eds/ (EDS repository sources, e.g. eds/wknd-site-abc containing blocks, models, fstab.yaml, xwalk.json)
├── docs/ (Extensive architecture, ADRs, migration guides, rules)
└── .agents/ (Agent skills and rules)
```

### 2.2 Existing Java OSGi & AI Architecture (`core/`)
- **Package Base:** `com.adobe.aem.modernizer.*`
- **Existing AI Gateway:** `com.adobe.aem.modernizer.ai.AiGateway`
  - Manages provider dispatch (`anthropic`, `openai`, `gemini`, `ollama`, `tokenrouter`, `mock`).
  - Supports model capabilities, pricing estimation, token tracking, system prompts, secret management (`EnvSecretProvider`).
- **Existing Agent Chat Loop:** `com.adobe.aem.modernizer.ai.chat.ChatAgentRuntime` & `ChatToolRegistry`
  - Implements bounded agent loop with basic tools.
  - Needs evolution into the production-grade, RAG-grounded, policy-enforced, citation-backed Chat Agent with comprehensive Tool Registry and AEM/JCR awareness.
- **Existing Persistence Layer:** `com.adobe.aem.modernizer.persistence.*`
  - `Store.java`: Storage interface for projects, jobs, plans, generated files, validation results.
  - `JcrStore.java`: JCR-backed persistence under `/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}` using Sling Repository.
  - `InMemoryStore.java`: In-memory baseline for unit tests and local mock runs.
- **Existing Servlets & Router:** `com.adobe.aem.modernizer.dashboard.*`
  - `DashboardApi.java`: Serves `/bin/aem-eds-modernizer/api/*`.
  - `ApiRouter.java`: Comprehensive JSON REST API routing for dashboard, projects, jobs, migrations, dry-run, and chat.
  - `ModernizerHomeServlet.java`, `DryRunServlet.java`, `MigrationServlet.java`, `ProjectServlet.java`.
- **Existing Specialized Agents (32 agents):**
  - `DiscoveryAgent`, `ContentAnalysisAgent`, `ComponentMappingAgent`, `BlockGenerationAgent`, `ValidationAgent`, `SelfRepairAgent`, `AdvancedVisualValidationAgent`, etc.

### 2.3 Existing EDS Knowledge Source (`eds/wknd-site-abc`)
- Contains live EDS repository files:
  - Block directories (`blocks/accordion`, `blocks/cards`, `blocks/carousel`, `blocks/columns`, `blocks/embed`, `blocks/footer`, `blocks/form`, `blocks/header`, `blocks/hero`, `blocks/modal`, `blocks/navigation`, `blocks/quote`, `blocks/table`, `blocks/tabs`, `blocks/teaser`, `blocks/title`, `blocks/video`, etc.)
  - Universal Editor & Model definitions: `component-definition.json`, `component-models.json`, `component-filters.json`, `fstab.yaml`, `helix-query.yaml`, `helix-sitemap.yaml`, `xwalk.json`, `head.html`.
  - Scripts & Styles: `scripts/scripts.js`, `scripts/aem.js`, `styles/styles.css`.
  - Documentation & conventions: `README.md`, `AGENTS.md`, `docs/`.

---

## 3. Findings & Architectural Fit for RAG + AI Chat Agent

1. **Native AEM Execution:**
   - The project runs natively inside AEM OSGi runtime as well as standalone fat JAR for testing.
   - All state persists to JCR (`/var/aem-eds-modernizer` and new `/var/modernizer/rag`).
   - No external Python, Node.js, Redis, or PostgreSQL services are needed.

2. **Integration with Existing AI Gateway:**
   - `AiGateway` is already registered as an OSGi service (`@Component(service = AiGateway.class)`).
   - Embeddings provider abstraction (`EmbeddingProvider`) and vector store abstraction (`VectorStore`) can plug directly into `AiGateway` and OSGi service registry.

3. **RepoInit & Security:**
   - RepoInit script in `ui.config` already sets up `modernizer-service` with permissions on `/content`, `/conf`, `/apps`, and `/var/aem-eds-modernizer`.
   - We will extend RepoInit to ensure `/var/modernizer/rag` permissions are configured.

4. **Sling Jobs & Checkpointed Ingestion:**
   - Background indexing and synchronization will use Sling Jobs (`org.apache.sling.event.jobs.JobConsumer` / `JobExecutor`) ensuring resilience, cluster-safety, and Cloud Manager compliance.

5. **AEM Chat Agent & Dashboard Integration:**
   - Chat endpoint `POST /bin/modernizer/chat` and API router routes will connect to the new `ChatAgent`, `RetrievalService`, `PolicyEngine`, and `ToolRegistry`.
   - Dashboard UI in `ui.apps` (clientlib-dashboard `dashboard.js` & `dashboard.css`, and HTL `home.html`) will integrate RAG search, citation tooltips, confidence meters, evaluation tabs, and tool execution confirmations.
