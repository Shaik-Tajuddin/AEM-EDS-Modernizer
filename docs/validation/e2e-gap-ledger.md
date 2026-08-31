# E2E Gap Ledger — IDE Cloud Chat Audit

**Date:** 2026-09-01  
**URL:** http://localhost:4502/content/aem-eds-modernizer/home.html/  
**Project:** `wknd-site-aboutus`  
**Methods:** cursor-ide-browser snapshot, curl API, content-modeling spot-check on generated block JS

## Evidence summary

| Check | Result | Evidence |
| --- | --- | --- |
| Live Event Stream & Audit Log removed | PASS | Nav snapshot has Overview…Benchmarks + Agent Chat; no Live Events; HTML has `#terminal` / Real-time Activity Stream only |
| Overview owns activity stream | PASS | Overview card + `#terminal`; chat chip “Overview stream” opens Overview |
| Local/Cloud AI providers in Setup | PASS | Select options: antigravity, cursor, claudecode, geminicode, anthropic, openai, gemini, ollama |
| Agent Chat tab (StaticDashboard) | PASS (after fix) | Was missing on content URL (servlet uses StaticDashboard, not HTL); added nav + tab + `sendChat` |
| Chat API real tools (IDE mode) | PASS | `POST …/chat` with `show status` → `provider=antigravity`, `model=ide-handoff`, `steps=[{tool:project_status}]` |
| Dry-run pipeline | PASS | `POST …/dryrun` → `state=COMPLETED`, `mode=DRY_RUN` |
| Reconcile create/leave/enhance | PARTIAL | Event: `title=LEAVE text=LEAVE experiencefragment=LEAVE \| create=0 leave=3 enhance=0` (filesystem already had blocks/) |
| Components badges | PARTIAL | Client JS applies Created/Left/Enhanced from reconcile event; not visually asserted this run |
| `getHtmlFromBlockRow` purge in generators | PASS | New generator/mock use `getHtmlFromRow`; heal rewrites legacy |
| Content-modeling spot-check | PASS (sampled) | `blocks/accordion|breadcrumb|button/*.js` use `getHtmlFromRow`; no old helper |

## Findings

### P1 — Flow / correctness

1. **Content URL uses StaticDashboard, not HTL `home.html`**  
   - **Evidence:** `/content/aem-eds-modernizer/home.html/` is served by `ModernizerHomeServlet` → `StaticDashboard.html()`; HTL Agent Chat / chip edits alone do not appear.  
   - **Fix applied:** Agent Chat added to `StaticDashboard`.  
   - **Follow-up:** Prefer one UI source (HTL *or* StaticDashboard) to avoid dual maintenance.

2. **Reconcile LEAVE is overly aggressive when local `blocks/` already exists**  
   - **Evidence:** Dry-run reconcile `create=0 leave=3` despite mapped components needing fresh rootpath fidelity.  
   - **Impact:** IDE handoff scaffolds may not regenerate.  
   - **Fix:** Scope LEAVE to EDS Git listing / content-hash mismatch; do not treat Modernizer workspace leftovers as authoritative EDS parity.

### P2 — Material drag

3. **Stale generated files in store may still mention old helpers**  
   - **Evidence:** Event dump / prior job payloads referenced `getHtmlFromBlockRow` in older content; freshly sampled JS files use `getHtmlFromRow`.  
   - **Fix:** Re-run Generate Blocks or purge prior job files after helper rename.

4. **Vanity paths `/aem-eds-modernizer` / `.html` return 403 with trailing slash**  
   - **Evidence:** Browser navigate to `/aem-eds-modernizer/` → DefaultGetServlet 403; content path works.  
   - **Fix:** Document content URL as canonical; or fix servlet suffix handling.

5. **Deploy race: brief 409 on `/bin/aem-eds-modernizer/api` after package install**  
   - **Evidence:** Immediate post-deploy POST returned 409 “repository state conflicting”; recovered after ~8s.  
   - **Fix:** Retry/backoff in operators’ scripts; not a product defect if transient.

### P3 — Polish

6. **Chat UI messages lack strong a11y names**  
   - **Evidence:** After Status click, snapshot did not expose chat bubble text as named nodes (log-terminal div).  
   - **Fix:** Use structured chat bubbles with roles/labels (as in HTL `home.html` chat card).

7. **ENHANCE path rarely fires**  
   - **Evidence:** No `ENHANCE` decisions in dry-run event; mismatch detection is marker-based only.  
   - **Fix:** Compare EDS JS/content model to rootpath contract for real enhance.

## Content-modeling notes (sampled)

- Sampled blocks (`accordion`, `breadcrumb`, `button`) use `getHtmlFromRow` / semantic extractConfig.  
- Recommend validating ≤4 cells/row and no spreadsheet headers on next CREATE run for `title` / `text` once LEAVE logic is narrowed.

## Assumptions

- Canonical operator URL is `http://localhost:4502/content/aem-eds-modernizer/home.html/`.  
- Project `wknd-site-aboutus` with `aiProvider=antigravity` is representative for IDE mode.  
- AEM MCP JCR tools were not required once Author API + browser confirmed state.

## Next levers

1. Unify dashboard UI onto one renderer.  
2. Fix reconcile LEAVE source-of-truth.  
3. Harden enhance mismatch detection against rootpath.
