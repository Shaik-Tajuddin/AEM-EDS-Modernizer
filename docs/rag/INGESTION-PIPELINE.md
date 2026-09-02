# RAG Ingestion Pipeline & Knowledge Maintenance Runbook

## 1. Pipeline Overview

The AEM-EDS-Modernizer RAG Ingestion Pipeline continuously analyzes, parses, chunks, embeds, and shards Edge Delivery Services repository artifacts into AEM's JCR repository.

```
[EDS Repository Scanner]
   ├── Local Filesystem: eds/<project>/
   └── Remote GitHub: RealGitHubClient Git Trees API
           │
           ▼
[Change Detection (SHA-256 Fingerprint)]
   ├── Match: Skip (Cached in JCR)
   └── Mismatch / New: Mark for Parsing
           │
           ▼
[Tiered Semantic Chunker]
   ├── Markdown: Heading Hierarchy (#, ##, ###)
   ├── Block JS: AST Function / decorate() Boundaries
   ├── Block CSS: Rule & Media Query Blocks
   └── JSON Models: Universal Editor Definitions & Models
           │
           ▼
[AI Gateway Embedding Dispatch]
   ├── OpenAI (text-embedding-3-small, 1536d)
   ├── Ollama (nomic-embed-text, 768d)
   └── TokenRouter: Fallback & Cost Guardrails
           │
           ▼
[Persistence & Vector Indexing]
   ├── Sharded JCR Store: /var/modernizer/rag/projects/{id}/chunks/{prefix}/{chunkId}
   └── Memory Index: ConcurrentHashMap<String, VectorEntry> for <10ms Cosine Math
```

---

## 2. Ingestion Triggering & Sling Job Execution

### Asynchronous Background Sync (Sling Job)
Ingestion runs asynchronously using the Apache Sling Job Manager to prevent HTTP timeouts when indexing large repositories:

- **Topic**: `com/adobe/aem/modernizer/rag/sync`
- **Consumer**: `com.adobe.aem.modernizer.rag.sync.RagSyncJobConsumer`
- **Checkpointing**: Saves sync state every 10 documents, allowing seamless resumption if an OSGi bundle restarts during ingestion.

### REST Trigger Endpoint:
```http
POST /bin/modernizer/rag/sync
Content-Type: application/x-www-form-urlencoded
CSRF-Token: <valid-token>

projectId=wknd-site&forceReindex=false
```

### Direct Status Query:
```http
GET /bin/modernizer/rag/sync?projectId=wknd-site
```

Response:
```json
{
  "syncId": "sync-1788369000123",
  "status": "COMPLETED",
  "documentsDiscovered": 38,
  "documentsProcessed": 38,
  "chunksCreated": 142,
  "embeddingsGenerated": 142,
  "errorsCount": 0,
  "startedAt": "2026-09-02T16:45:00Z",
  "completedAt": "2026-09-02T16:45:18Z"
}
```

---

## 3. Tiered Semantic Chunking Rules

| Document Type | Source Pattern | Chunking Boundary | Chunk Type | Metadata Preserved |
| :--- | :--- | :--- | :--- | :--- |
| **Markdown** | `docs/**/*.md`, `README.md` | Markdown Headings (`#`, `##`, `###`) | `MARKDOWN_SECTION` | Breadcrumb hierarchy, startLine, endLine |
| **Block JavaScript** | `blocks/**/*.js` | Decorator function `decorate(block)` or named exports | `EDS_JS_DECORATOR` | Block name, variant handlers, DOM selectors |
| **Block CSS** | `blocks/**/*.css` | Top-level block rule sets and media queries | `EDS_CSS_RULES` | Block name, selector specificity |
| **Component Model** | `models/*.json`, `component-*.json` | Individual definition/model objects | `EDS_MODEL_JSON` | Model title, ID, fields, plugins |
| **Fstab / Config** | `fstab.yaml`, `helix-query.yaml` | Top-level YAML stanzas | `CONFIG_YAML` | Mountpoint type, query paths |

---

## 4. Re-Indexing & Cache Invalidation

### Force Re-index:
To re-embed and rebuild all vectors (e.g. after updating embedding models or clearing corrupt JCR state):
```bash
curl -u admin:admin -X POST "http://localhost:4502/bin/modernizer/rag/sync" \
  -d "projectId=wknd-site&forceReindex=true"
```

### JCR Node Cleanup:
If necessary, RAG data can be purged via CRXDE Lite or cURL:
```bash
curl -u admin:admin -X DELETE "http://localhost:4502/var/modernizer/rag/projects/wknd-site"
```
Upon the next sync run, the ingestion pipeline will recreate the `/var/modernizer/rag/projects/wknd-site` tree and re-index from scratch.
