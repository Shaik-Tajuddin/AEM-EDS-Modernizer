# RAG-Grounded AI Chat Agent User & Developer Guide

## 1. Overview & Capabilities

The **AEM-EDS-Modernizer Grounded AI Chat Agent** provides interactive guidance, code generation, diagnostics, and migration operations directly inside the AEM Author Dashboard (`/apps/aem-eds-modernizer/content/home.html` or `/bin/modernizer/home`).

### Supported Workflows:
1. **EDS Block & Development Guidance**:
   - Query: *"How do I author a Hero block in this repository?"*
   - Response: Explains authoring table structures, CSS variants (`.dark`, `.centered`), and DOM decoration rules based on the live `blocks/hero/` implementation.
2. **Universal Editor Model Inquiries**:
   - Query: *"What fields are defined in the Teaser component model?"*
   - Response: Inspects `component-models.json` and details authoring properties, default values, and asset picker rules.
3. **Migration Diagnostics**:
   - Query: *"Why did validation fail on my last run?"*
   - Response: Retrieves validation logs, analyzes DOM structural mismatches or missing style classes, and proposes automated healing.
4. **Action Proposal & Execution Gating**:
   - Query: *"Run a dry run on this project."*
   - Response: Gated under RBAC. Agent returns a confirmation prompt (`requiresConfirmation: true`). Once confirmed by an authorized operator, triggers `Orchestrator.runDryRun(...)`.

---

## 2. API Contract & Invocation

### Endpoint:
```http
POST /bin/modernizer/chat
Content-Type: application/json
CSRF-Token: <token>

{
  "projectId": "wknd-site",
  "message": "How do I author a Hero block in this repository?",
  "conversationId": "conv-101",
  "action": null,
  "confirmed": false
}
```

### Response Structure:
```json
{
  "conversationId": "conv-101",
  "reply": "The Hero block is authored as a single-row table containing an image and a heading...",
  "confidence": 0.92,
  "confidenceLevel": "HIGH",
  "citations": [
    {
      "index": 1,
      "source": "blocks/hero/hero.js",
      "section": "Hero Block Decorator",
      "path": "blocks/hero/hero.js",
      "snippet": "export default function decorate(block) { ... }",
      "relevanceScore": 0.95
    }
  ],
  "requiresConfirmation": false,
  "confirmationPrompt": null,
  "suggestedActions": [
    {
      "label": "Inspect Hero Model",
      "prompt": "Show component-models.json definition for hero"
    }
  ]
}
```

---

## 3. Operator Confirmation Workflow

When an action modifies repository content or triggers compute-intensive pipelines:
1. User requests: *"Migrate page /content/wknd/us/en"*
2. Agent detects mutating action and returns:
   ```json
   {
     "reply": "Migrating page /content/wknd/us/en will create/overwrite documents in the repository. Please confirm execution.",
     "requiresConfirmation": true,
     "confirmationPrompt": "Confirm migration of page /content/wknd/us/en?"
   }
   ```
3. Dashboard displays an approval banner: **[Confirm & Execute]** or **[Cancel]**.
4. Upon clicking confirm, the client re-posts with:
   ```json
   {
     "projectId": "wknd-site",
     "action": "migratePage",
     "actionParams": { "path": "/content/wknd/us/en" },
     "confirmed": true
   }
   ```
5. Policy Engine verifies user credentials (`admin` or `operator`) and executes the migration through the live Orchestrator.
