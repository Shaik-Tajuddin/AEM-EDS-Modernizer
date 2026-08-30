let currentProjectId = "wknd-site";
let projectsList = [];

let csrfTokenPromise = null;
function getCsrfToken() {
  if (!csrfTokenPromise) {
    csrfTokenPromise = fetch("/libs/granite/csrf/token.json")
      .then((r) => r.json())
      .then((d) => (d && d.token ? d.token : ""))
      .catch(() => "");
  }
  return csrfTokenPromise;
}

const api = async (path, opts = {}) => {
  const cleanPath = path.startsWith("/") ? path.substring(1) : path;
  const url =
    "/bin/aem-eds-modernizer/api?path=" + encodeURIComponent(cleanPath);

  const headers = Object.assign(
    { "Content-Type": "application/json" },
    opts.headers || {},
  );
  if (opts.method && opts.method.toUpperCase() !== "GET") {
    const token = await getCsrfToken();
    if (token) {
      headers["CSRF-Token"] = token;
    }
  }
  opts.headers = headers;

  const res = await fetch(url, opts);
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`HTTP ${res.status}: ${(errText || "").substring(0, 200)}`);
  }
  return res.json();
};

function showToast(msg) {
  const t = document.getElementById("toast");
  if (!t) return;
  t.innerText = msg;
  t.classList.add("show");
  setTimeout(() => t.classList.remove("show"), 3500);
}

function showTab(tabId) {
  document
    .querySelectorAll(".nav-tab")
    .forEach((t) => t.classList.remove("active"));
  document
    .querySelectorAll(".tab-content")
    .forEach((c) => (c.style.display = "none"));
  const btn = Array.from(document.querySelectorAll(".nav-tab")).find(
    (b) =>
      b.getAttribute("onclick") && b.getAttribute("onclick").includes(tabId),
  );
  if (btn) btn.classList.add("active");
  const target = document.getElementById("tab-" + tabId);
  if (target) target.style.display = "block";
  if (tabId === "chat") loadChatHistory();
}

function setPipelineStep(stepId, state) {
  const el = document.getElementById("step-" + stepId);
  if (el) {
    if (state === "done") {
      el.className = "step-item done";
    } else if (state === "active") {
      el.className = "step-item active";
    } else {
      el.className = "step-item";
    }
  }
}

function log(agent, msg) {
  const term = document.getElementById("terminal");
  const time = new Date().toLocaleTimeString();
  const line = `<div class="log-line"><span class="log-time">[${time}]</span><span class="log-agent">[${agent}]</span><span>${msg}</span></div>`;
  if (term) {
    term.innerHTML += line;
    term.scrollTop = term.scrollHeight;
  }
}

function onProviderChange() {
  const provider = document.getElementById("cfg-aiProvider").value;
  const modelInput = document.getElementById("cfg-aiModel");
  const banner = document.getElementById("antigravity-banner");
  const modelGroup = document.getElementById("ai-model-group");
  const budgetGroup = document.getElementById("ai-budget-group");

  // Toggle Antigravity banner + hide irrelevant fields
  const isAntigravity = provider === "antigravity";
  if (banner) banner.style.display = isAntigravity ? "block" : "none";
  if (modelGroup) modelGroup.style.display = isAntigravity ? "none" : "";
  if (budgetGroup) budgetGroup.style.display = isAntigravity ? "none" : "";
  if (modelInput && !isAntigravity) {
    if (provider === "anthropic")
      modelInput.value = "claude-3-5-sonnet-20241022";
    else if (provider === "openai") modelInput.value = "gpt-4o";
    else if (provider === "gemini") modelInput.value = "gemini-1.5-pro";
    else if (provider === "ollama") modelInput.value = "qwen3:8b";
  }
  if (modelInput && isAntigravity) modelInput.value = "";
}

function loadWkndPreset() {
  document.getElementById("cfg-id").value = "wknd-site";
  document.getElementById("cfg-name").value = "WKND Site Modernization";
  document.getElementById("cfg-authorUrl").value = "http://localhost:4502";
  document.getElementById("cfg-publishUrl").value = "http://localhost:4503";
  document.getElementById("cfg-contentRoot").value =
    "/content/wknd/language-masters/en/adventures/ski-touring-mont-blanc";
  document.getElementById("cfg-pageScope").value = "/content/wknd/*";
  document.getElementById("cfg-repoUrl").value =
    "https://github.com/my-org/wknd-eds";
  document.getElementById("cfg-branch").value = "main";
  document.getElementById("cfg-markerProp").value = "edsModernize";
  document.getElementById("cfg-markerVal").value = "true";
  document.getElementById("cfg-authoringStrategy").value = "UNIVERSAL_EDITOR";
  document.getElementById("cfg-aiProvider").value = "antigravity";
  document.getElementById("cfg-aiModel").value = "";
  document.getElementById("cfg-maxBudget").value = "0.00";
  document.getElementById("cfg-maxRepair").value = "5";
  onProviderChange(); // trigger banner and field visibility
  showToast("Loaded WKND Site Configuration Preset (✨ Antigravity mode)");
}

function clearForm() {
  document.getElementById("cfg-id").value =
    "project-" + Math.random().toString(36).substring(2, 7);
  document.getElementById("cfg-name").value = "";
  document.getElementById("cfg-authorUrl").value = "http://localhost:4502";
  document.getElementById("cfg-publishUrl").value = "";
  document.getElementById("cfg-contentRoot").value = "/content/";
  document.getElementById("cfg-pageScope").value = "";
  document.getElementById("cfg-repoUrl").value = "";
  document.getElementById("cfg-figmaUrl").value = "";
}

async function saveProjectConfig() {
  const payload = {
    id: document.getElementById("cfg-id").value.trim() || "project-1",
    name:
      document.getElementById("cfg-name").value.trim() || "Untitled Project",
    aemAuthorUrl: document.getElementById("cfg-authorUrl").value.trim(),
    aemPublishUrl: document.getElementById("cfg-publishUrl").value.trim(),
    contentRoot: document.getElementById("cfg-contentRoot").value.trim(),
    pageScope: document.getElementById("cfg-pageScope").value.trim(),
    edsGitRepoUrl: document.getElementById("cfg-repoUrl").value.trim(),
    edsBranch: document.getElementById("cfg-branch").value.trim(),
    figmaUrl: document.getElementById("cfg-figmaUrl").value.trim(),
    markerProperty: document.getElementById("cfg-markerProp").value.trim(),
    markerValue: document.getElementById("cfg-markerVal").value.trim(),
    authoringStrategy: document.getElementById("cfg-authoringStrategy").value,
    aiProvider: document.getElementById("cfg-aiProvider").value,
    aiModel: document.getElementById("cfg-aiModel").value.trim(),
    maxBudgetUsd:
      parseFloat(document.getElementById("cfg-maxBudget").value) || 100.0,
    maxRepairAttempts:
      parseInt(document.getElementById("cfg-maxRepair").value, 10) || 5,
  };

  log(
    "connection",
    `Saving project '${payload.name}' (${payload.id}) & verifying AEM endpoint...`,
  );
  try {
    const saved = await api("projects", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    currentProjectId = saved.id;
    log(
      "connection",
      `Project saved successfully: ContentRoot=${payload.contentRoot}, EDS Repo=${payload.edsGitRepoUrl}`,
    );
    showToast(`Project '${saved.name}' Saved & Connected!`);
    document.getElementById("btn-dryrun").disabled = false;
    setPipelineStep("connect", "done");
    setPipelineStep("dryrun", "active");
    await loadProjectsList();
    showTab("overview");
  } catch (err) {
    log("error", `Failed to save project: ${err.message}`);
    showToast("Error saving project");
  }
}

async function loadProjectsList() {
  try {
    projectsList = await api("projects");
    const select = document.getElementById("project-select");
    if (select && projectsList && projectsList.length > 0) {
      select.innerHTML =
        projectsList
          .map(
            (p) =>
              `<option value="${p.id}" ${p.id === currentProjectId ? "selected" : ""}>${p.name || p.id}</option>`,
          )
          .join("") + '<option value="new">+ Create New Project...</option>';
    }
  } catch (e) {
    console.log(e);
  }
}

async function deleteCurrentProject() {
  if (!currentProjectId) return;
  const p = projectsList.find((x) => x.id === currentProjectId);
  const label = p ? (p.name || p.id) : currentProjectId;
  if (!confirm(`Delete project "${label}"?\n\nThis removes its saved config, jobs, inventory and generated blocks. This cannot be undone.`)) return;

  try {
    await api(`projects/${encodeURIComponent(currentProjectId)}`, { method: "DELETE" });
    showToast(`🗑️ Project '${label}' deleted`);
    projectsList = projectsList.filter((x) => x.id !== currentProjectId);
    currentProjectId = (projectsList && projectsList.length > 0) ? projectsList[0].id : "wknd-site";
    chatHistoryLoaded = false;
    chatHistory = [];
    await loadProjectsList();
    if (projectsList && projectsList.length > 0) {
      await populateFormFromProject(currentProjectId);
      await refreshDashboard();
    }
    showTab("overview");
  } catch (err) {
    log("error", `Failed to delete project: ${err.message}`);
    showToast("Error deleting project: " + err.message);
  }
}

async function onProjectSelectChange() {
  const val = document.getElementById("project-select").value;
  if (val === "new") {
    clearForm();
    showTab("setup");
  } else {
    currentProjectId = val;
    await populateFormFromProject(val);
    await refreshDashboard();
  }
}

async function populateFormFromProject(id) {
  const p =
    projectsList.find((x) => x.id === id) || (await api(`projects/${id}`));
  if (p) {
    document.getElementById("cfg-id").value = p.id || "";
    document.getElementById("cfg-name").value = p.name || "";
    document.getElementById("cfg-authorUrl").value =
      p.aemAuthorUrl || "http://localhost:4502";
    document.getElementById("cfg-publishUrl").value = p.aemPublishUrl || "";
    document.getElementById("cfg-contentRoot").value =
      p.contentRoot || "/content/wknd";
    document.getElementById("cfg-pageScope").value = p.pageScope || "";
    document.getElementById("cfg-repoUrl").value = p.edsGitRepoUrl || "";
    document.getElementById("cfg-branch").value = p.edsBranch || "main";
    document.getElementById("cfg-figmaUrl").value = p.figmaUrl || "";
    document.getElementById("cfg-markerProp").value =
      p.markerProperty || "edsModernize";
    document.getElementById("cfg-markerVal").value = p.markerValue || "true";
    document.getElementById("cfg-authoringStrategy").value =
      p.authoringStrategy || "UNIVERSAL_EDITOR";
    document.getElementById("cfg-aiProvider").value =
      p.aiProvider || "anthropic";
    document.getElementById("cfg-aiModel").value =
      p.aiModel || "claude-3-5-sonnet-20241022";
    document.getElementById("cfg-maxBudget").value = p.maxBudgetUsd || 100.0;
    document.getElementById("cfg-maxRepair").value = p.maxRepairAttempts || 5;
  }
}

let generatedFiles = [];
let blockFilesMap = {};
let activeBlockName = null;
let activeFileTab = "demo";

async function runDryRun() {
  const btn = document.getElementById("btn-dryrun");
  if (btn) btn.disabled = true;
  setPipelineStep("connect", "done");
  setPipelineStep("dryrun", "active");
  log(
    "orchestrator",
    `Starting Mandatory Dry Run for project '${currentProjectId}'...`,
  );
  try {
    const job = await api(`projects/${currentProjectId}/dryrun`, {
      method: "POST",
    });
    log("orchestrator", `Dry Run execution completed with state: ${job.state}`);
    setPipelineStep("dryrun", "done");
    setPipelineStep("build", "active");
    await refreshDashboard();
    const btnMigrate = document.getElementById("btn-migrate");
    if (btnMigrate) btnMigrate.disabled = false;
    showToast("Dry Run Completed! Discovered pages and mapped components.");
  } catch (err) {
    log("error", `Dry run failed: ${err.message}`);
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function runMigration() {
  const btnMigrate = document.getElementById("btn-migrate");
  if (btnMigrate) btnMigrate.disabled = true;
  setPipelineStep("dryrun", "done");
  setPipelineStep("build", "active");
  log(
    "orchestrator",
    `Building & Generating EDS Block Quad & Content for project '${currentProjectId}'...`,
  );
  try {
    const job = await api(`projects/${currentProjectId}/migrate`, {
      method: "POST",
    });
    log("orchestrator", `Block Generation finished with state: ${job.state}`);
    setPipelineStep("build", "done");
    setPipelineStep("validate", "active");
    await refreshDashboard();

    // Enable the final Commit & Push button so the operator has time to inspect blocks
    const btnPublish = document.getElementById("btn-publish");
    if (btnPublish) btnPublish.disabled = false;

    showTab("components");
    showToast(
      "⚡ Blocks Generated! Please inspect and validate them before committing to Git.",
    );
  } catch (err) {
    log("error", `Generation failed: ${err.message}`);
  } finally {
    if (btnMigrate) btnMigrate.disabled = false;
  }
}

async function runPushToGit() {
  const btnPublish = document.getElementById("btn-publish");
  if (btnPublish) btnPublish.disabled = true;
  setPipelineStep("validate", "done");
  setPipelineStep("publish", "active");
  log(
    "publishing",
    `🚀 Committing and Pushing generated blocks & models to remote Git repository...`,
  );
  try {
    const job = await api(`projects/${currentProjectId}/publish`, {
      method: "POST",
    });
    log(
      "publishing",
      `Successfully committed blocks to preview branch and opened Pull Request! State: ${job.state}`,
    );
    setPipelineStep("publish", "done");
    await refreshDashboard();
    showToast("🚀 Successfully committed and pushed blocks to GitHub!");
  } catch (err) {
    log("error", `Git push failed: ${err.message}`);
    showToast("Error pushing to Git: " + err.message);
  } finally {
    if (btnPublish) btnPublish.disabled = false;
  }
}

async function refreshDashboard() {
  try {
    const inv = await api(`projects/${currentProjectId}/inventory`);
    if (inv && inv.pages) {
      document.getElementById("stat-pages").innerText = inv.pages.length;
      document.getElementById("stat-eligible").innerText =
        (inv.eligiblePages || inv.pages.length) + " eligible";
      document.getElementById("stat-components").innerText = inv.components
        ? inv.components.length
        : 0;
      renderPagesTable(inv.pages);
      renderComponentsTable(inv.components);
    }
  } catch (e) {}

  try {
    const files = await api(`projects/${currentProjectId}/files`);
    if (files) {
      generatedFiles = files;
      processBlockFiles(files);
      renderBlockList();
      if (activePagePath) {
        selectPageRow(activePagePath);
      }
    }
  } catch (e) {}

  try {
    const plan = await api(`projects/${currentProjectId}/plan`);
    if (plan) {
      document.getElementById("stat-cost").innerText =
        "$" + (plan.costExpected || 0).toFixed(2);
      document.getElementById("stat-requests").innerText =
        (plan.aiRequestsExpected || 0) + " AI calls estimated";
      document.getElementById("stat-time").innerText =
        (plan.timeExpectedSec || 0) + "s";
      document.getElementById("stat-range").innerText =
        `Lo: ${plan.timeOptimisticSec || 0}s | Hi: ${plan.timePessimisticSec || 0}s`;
      if (plan.derivationTrail) {
        document.getElementById("estimate-trail").innerText =
          plan.derivationTrail.join("\n");
      }
    }
  } catch (e) {}

  try {
    const redirects = await api(`projects/${currentProjectId}/redirects`);
    renderRedirectsTable(redirects);
  } catch (e) {}

  try {
    const deps = await api(`projects/${currentProjectId}/dependencies`);
    renderDependenciesTable(deps);
  } catch (e) {}

  try {
    const rollout = await api(`projects/${currentProjectId}/rollout-stages`);
    renderRolloutTable(rollout);
  } catch (e) {}

  try {
    const repairs = await api(`projects/${currentProjectId}/repairs`);
    renderRepairsTable(repairs);
  } catch (e) {}

  try {
    const benchmarks = await api(`projects/${currentProjectId}/benchmarks`);
    renderBenchmarksTable(benchmarks);
  } catch (e) {}

  try {
    const events = await api(`projects/${currentProjectId}/events`);
    if (events && Array.isArray(events)) {
      const eventsLog = document.getElementById("terminal");
      if (eventsLog) {
        eventsLog.innerHTML = events
          .map((e) => {
            const time = new Date(
              e.timestamp || Date.now(),
            ).toLocaleTimeString();
            const ag = e.agent || e.level || "system";
            const isAiReq =
              ag === "ai-request" ||
              (e.message && e.message.includes("📤 REQUEST:"));
            const isAiResp =
              ag === "ai-response" ||
              (e.message && e.message.includes("📥 RESPONSE"));
            const isAi = isAiReq || isAiResp || ag.startsWith("ai-");

            let lineClass = "log-line";
            if (isAiReq) lineClass += " log-ai-request";
            else if (isAiResp) lineClass += " log-ai-response";
            else if (isAi) lineClass += " log-ai";

            const formattedMessage = (e.message || "")
              .replace(/&/g, "&amp;")
              .replace(/</g, "&lt;")
              .replace(/>/g, "&gt;")
              .replace(/\n/g, "<br/>");

            return `<div class="${lineClass}"><span class="log-time">[${time}]</span><span class="log-agent">[${ag}]</span><span class="log-msg">${formattedMessage}</span></div>`;
          })
          .join("");
        eventsLog.scrollTop = eventsLog.scrollHeight;
      }
    }
  } catch (e) {}
}

function processBlockFiles(files) {
  blockFilesMap = {};
  if (!files || files.length === 0) return;

  files.forEach((f) => {
    const path = f.path || "";
    if (path.startsWith("blocks/")) {
      const parts = path.split("/");
      if (parts.length >= 3) {
        const bName = parts[1];
        const fileName = parts[parts.length - 1];
        if (!blockFilesMap[bName]) {
          blockFilesMap[bName] = { name: bName, files: {}, sourcePath: null };
        }
        // Keep the AEM root path reference so blocks and pages stay linked to the same JCR source
        if (f.sourcePath && !blockFilesMap[bName].sourcePath) {
          blockFilesMap[bName].sourcePath = f.sourcePath;
        }
        if (fileName.endsWith(".js")) blockFilesMap[bName].files.js = f;
        else if (fileName.endsWith(".css")) blockFilesMap[bName].files.css = f;
        else if (fileName.startsWith("_") && fileName.endsWith(".json"))
          blockFilesMap[bName].files.json = f;
        else if (
          fileName.endsWith("-example.html") ||
          fileName.endsWith(".html")
        )
          blockFilesMap[bName].files.demo = f;
        else if (fileName.toLowerCase() === "readme.md")
          blockFilesMap[bName].files.readme = f;
      }
    }
  });
}

function renderBlockList() {
  const container = document.getElementById("block-items-container");
  const countBadge = document.getElementById("blocks-count-badge");
  const totalCount = document.getElementById("block-list-total");
  const blockNames = Object.keys(blockFilesMap);

  if (countBadge)
    countBadge.innerText = `${blockNames.length} Blocks Generated`;
  if (totalCount) totalCount.innerText = blockNames.length;

  if (!container) return;
  if (blockNames.length === 0) {
    container.innerHTML =
      '<div style="padding:16px; color:var(--text-dim); font-size:0.85rem; text-align:center;">Run Dry Run or Generate Blocks to populate.</div>';
    return;
  }

  if (!activeBlockName || !blockFilesMap[activeBlockName]) {
    activeBlockName = blockNames[0];
  }

  container.innerHTML = blockNames
    .map((name) => {
      const b = blockFilesMap[name];
      const fileCount = Object.keys(b.files || {}).length;
      const isActive = name === activeBlockName;
      return `<div class="block-item ${isActive ? "active" : ""}" onclick="selectBlock('${name}')">
      <div class="block-item-title">
        <span>🧱</span>
        <span>${name}</span>
      </div>
      <span class="block-item-badge">${fileCount} files</span>
    </div>`;
    })
    .join("");

  renderActiveBlockDetail();
}

function selectBlock(name) {
  activeBlockName = name;
  renderBlockList();
}

function switchBlockFileTab(tabName) {
  activeFileTab = tabName;
  ["demo", "json", "js", "css", "readme"].forEach((t) => {
    const el = document.getElementById("filetab-" + t);
    if (el) {
      if (t === tabName) el.classList.add("active");
      else el.classList.remove("active");
    }
  });
  renderActiveBlockDetail();
}

function renderActiveBlockDetail() {
  if (!activeBlockName || !blockFilesMap[activeBlockName]) return;
  const b = blockFilesMap[activeBlockName];
  const fileObj = (b.files || {})[activeFileTab];

  const pathEl = document.getElementById("block-file-path");
  const codeContainer = document.getElementById("block-view-code");
  const codeContent = document.getElementById("block-code-content");
  const demoContainer = document.getElementById("block-view-demo");
  const demoRendered = document.getElementById("block-demo-rendered");

  if (pathEl) {
    pathEl.innerText = fileObj
      ? fileObj.path
      : `blocks/${activeBlockName}/[not found]`;
  }

  // Show the AEM root path this block was authored from (matches the Pages & Scope reference)
  const sourceRefEl = document.getElementById("block-source-ref");
  const sourcePathEl = document.getElementById("block-source-path");
  if (sourceRefEl && sourcePathEl) {
    if (b.sourcePath) {
      sourcePathEl.textContent = b.sourcePath;
      sourceRefEl.style.display = "block";
    } else {
      sourcePathEl.textContent = "—";
      sourceRefEl.style.display = "none";
    }
  }

  if (activeFileTab === "demo") {
    if (codeContainer) codeContainer.style.display = "none";
    if (demoContainer) demoContainer.style.display = "block";

    const htmlContent = b.files && b.files.demo ? b.files.demo.content : "";

    if (demoRendered) {
      if (htmlContent) {
        // Render the actual compiled HTML block inside a sandboxed iframe to prevent styles leaking
        const cleanHtml = htmlContent.replace(/"/g, '&quot;');
        demoRendered.innerHTML = `
          <div style="border-bottom:1px solid #e2e8f0; padding-bottom:12px; margin-bottom:18px;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <h3 style="margin:0; font-size:1.1rem; color:#0f172a;">👁️ Universal Editor Preview: <code>${activeBlockName}</code></h3>
              <span style="font-size:0.75rem; background:#dbeafe; color:#1d4ed8; padding:3px 8px; border-radius:4px; font-weight:700;">AEM UE Render</span>
            </div>
            <p style="margin:6px 0 0; font-size:0.82rem; color:#64748b;">
              This demonstrates how authors interact with the <b>${activeBlockName}</b> block when placed on a page.
            </p>
          </div>
          <iframe srcdoc="${cleanHtml}" style="width:100%; height:450px; border:1px solid #cbd5e1; border-radius:6px; background:#ffffff;"></iframe>
        `;
      } else {
        demoRendered.innerHTML = `
          <div style="text-align:center; padding:40px 20px; color:#64748b;">
            Select a generated block to view its Universal Editor rendered demo.
          </div>
        `;
      }
    }
  } else {
    if (demoContainer) demoContainer.style.display = "none";
    if (codeContainer) codeContainer.style.display = "flex";

    if (codeContent) {
      codeContent.innerText = fileObj
        ? fileObj.content
        : `// No ${activeFileTab} file generated for block '${activeBlockName}' yet.`;
    }
  }
}

function copyActiveCode() {
  const codeContent = document.getElementById("block-code-content");
  if (codeContent && codeContent.innerText) {
    navigator.clipboard
      .writeText(codeContent.innerText)
      .then(() => {
        showToast("📋 Code copied to clipboard!");
      })
      .catch(() => {
        showToast("Failed to copy code");
      });
  }
}

let activePagePath = "";
let activePageTab = "preview"; // preview, source

function renderPagesTable(pages) {
  const tbody = document.querySelector("#table-pages tbody");
  if (!tbody) return;
  tbody.innerHTML =
    pages && pages.length > 0
      ? pages
          .map(
            (p) =>
              `<tr data-path="${p.path}" onclick="selectPageRow('${p.path}')"><td style="word-break:break-all; padding: 10px;"><code>${p.path}</code></td><td>${p.title || "-"}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="2">No pages discovered yet.</td></tr>';

  if (pages && pages.length > 0 && !activePagePath) {
    selectPageRow(pages[0].path);
  }
}

function selectPageRow(path) {
  activePagePath = path;
  document.querySelectorAll("#table-pages tbody tr").forEach(tr => tr.classList.remove("active-row"));

  const tr = document.querySelector(`#table-pages tbody tr[data-path="${path}"]`);
  if (tr) tr.classList.add("active-row");

  const fileObj = generatedFiles.find(f => f.sourcePath === path && f.path.endsWith(".md"));
  const pathLabel = document.getElementById("page-preview-path");
  if (pathLabel) pathLabel.textContent = fileObj ? fileObj.path : "No migrated file found";

  renderPageBlockReferences(path);
  renderActivePageDetail();
}

/**
 * Populates the <eds-block-references> tag in the Pages & Scope tab with every
 * generated block that was authored from the same AEM root path as the page,
 * so page scope and block content share one identical JCR reference.
 */
function renderPageBlockReferences(pagePath) {
  const wrapper = document.getElementById("page-block-references");
  const list = document.getElementById("page-block-references-list");
  if (!wrapper || !list) return;

  if (!pagePath) {
    wrapper.style.display = "none";
    list.innerHTML = "";
    return;
  }

  const matches = Object.values(blockFilesMap).filter((b) => {
    if (!b.sourcePath) return false;
    return (
      b.sourcePath === pagePath ||
      pagePath.startsWith(b.sourcePath) ||
      b.sourcePath.startsWith(pagePath)
    );
  });

  if (matches.length === 0) {
    wrapper.style.display = "none";
    list.innerHTML = "";
    return;
  }

  list.innerHTML = matches
    .map(
      (b) =>
        `<button class="chat-chip" style="font-size:0.75rem; padding:4px 10px;" title="AEM root path: ${b.sourcePath}" onclick="jumpToBlock('${b.name}')">🧱 ${b.name}</button>`,
    )
    .join("");
  wrapper.style.display = "block";
}

/** Navigate from a page's block reference straight to the generated block inspector. */
function jumpToBlock(blockName) {
  showTab("components");
  selectBlock(blockName);
}

function switchPageFileTab(tab) {
  activePageTab = tab;
  document.querySelectorAll("#pagetab-preview, #pagetab-source, #pagetab-html").forEach(b => b.classList.remove("active"));
  const activeBtn = document.getElementById("pagetab-" + tab);
  if (activeBtn) activeBtn.classList.add("active");

  renderActivePageDetail();
}

function renderActivePageDetail() {
  const previewContainer = document.getElementById("page-view-preview");
  const sourceContainer = document.getElementById("page-view-source");
  const previewRendered = document.getElementById("page-preview-rendered");
  const sourceContent = document.getElementById("page-source-content");
  const htmlContainer = document.getElementById("page-view-html");
  const htmlRendered = document.getElementById("page-html-rendered");

  const fileObj = generatedFiles.find(f => f.sourcePath === activePagePath && f.path.endsWith(".md"));
  const markdown = fileObj ? fileObj.content : "";

  if (activePageTab === "preview") {
    if (sourceContainer) sourceContainer.style.display = "none";
    if (htmlContainer) htmlContainer.style.display = "none";
    if (previewContainer) previewContainer.style.display = "block";
    if (previewRendered) {
      if (markdown) {
        previewRendered.innerHTML = formatMarkdownToDA(markdown);
      } else {
        previewRendered.innerHTML = `
          <div style="text-align:center; padding:40px 20px; color:#64748b;">
            No migrated markdown content available for this page. Click <b>Migrate Site</b> to generate.
          </div>
        `;
      }
    }
  } else if (activePageTab === "html") {
    if (sourceContainer) sourceContainer.style.display = "none";
    if (previewContainer) previewContainer.style.display = "none";
    if (htmlContainer) htmlContainer.style.display = "block";
    if (htmlRendered) {
      if (markdown || activePagePath) {
        renderPageHtmlView(markdown);
      } else {
        htmlRendered.innerHTML = `
          <div style="text-align:center; padding:40px 20px; color:#64748b;">
            Select a page to view its complete authored HTML page.
          </div>
        `;
      }
    }
  } else {
    if (previewContainer) previewContainer.style.display = "none";
    if (htmlContainer) htmlContainer.style.display = "none";
    if (sourceContainer) sourceContainer.style.display = "flex";
    if (sourceContent) {
      sourceContent.innerText = markdown || "// No migrated markdown content available for this page.";
    }
  }
}

/**
 * Renders a complete HTML page composed ENTIRELY from the generated blocks authored
 * for this page's root path — no direct AEM page fetch. Each generated block's demo
 * markup and styles are assembled into the page, followed by the migrated DA content.
 */
function renderPageHtmlView(markdown) {
  const container = document.getElementById("page-html-rendered");
  if (!container) return;

  if (!activePagePath) {
    container.innerHTML = `
      <div style="text-align:center; padding:40px 20px; color:#64748b;">
        Select a page to view its complete authored HTML page.
      </div>
    `;
    return;
  }

  const blocks = getBlocksForPagePath(activePagePath);
  const doc = buildPageHtmlDocument(activePagePath, markdown);

  container.innerHTML =
    `<div style="margin-bottom:10px; font-size:0.8rem; color:#64748b;">🧱 Page composed from ${blocks.length} generated block(s) authored at <code style="background:#0f172a; color:#7dd3fc; padding:2px 6px; border-radius:4px;">${escapeAttr(activePagePath)}</code></div>` +
    `<iframe srcdoc="${doc.replace(/"/g, "&quot;")}" style="width:100%; min-height:600px; border:1px solid #cbd5e1; border-radius:6px; background:#ffffff;"></iframe>`;
}

function getBlocksForPagePath(pagePath) {
  return Object.values(blockFilesMap).filter((b) => {
    if (!b.sourcePath) return false;
    return (
      b.sourcePath === pagePath ||
      pagePath.startsWith(b.sourcePath) ||
      b.sourcePath.startsWith(pagePath)
    );
  });
}

/** Extracts the <style> blocks and <body> content from a generated block demo HTML. */
function extractBlockDemoParts(demoHtml) {
  if (!demoHtml) return { styles: "", body: "" };
  const styles = (demoHtml.match(/<style[\s\S]*?<\/style>/gi) || []).join("\n");
  let body = demoHtml;
  const bodyMatch = demoHtml.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
  if (bodyMatch) body = bodyMatch[1];
  else body = demoHtml.replace(/<html[^>]*>|<\/html>|<head[\s\S]*?<\/head>|<!doctype[^>]*>/gi, "");
  body = body.replace(/<script[\s\S]*?<\/script>/gi, "");
  return { styles, body };
}

function escapeAttr(text) {
  return (text || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function buildPageHtmlDocument(pagePath, markdown) {
  const blocks = getBlocksForPagePath(pagePath);
  const pageTitle = (pagePath ? pagePath.split("/").filter(Boolean).pop() : "page")
    .replace(/-+/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());

  let blockStyles = "";
  let blockSections = "";
  blocks.forEach((b) => {
    const demo = b.files && b.files.demo ? b.files.demo.content : "";
    const { styles, body } = extractBlockDemoParts(demo);
    blockStyles += styles + "\n";
    blockSections +=
      `<section class="page-block-section" data-block="${escapeAttr(b.name)}">\n` +
      `<div class="page-block-tag">🧱 ${escapeAttr(b.name)} — authored from ${escapeAttr(b.sourcePath)}</div>\n` +
      (body || `<div class="page-block-empty">No authored demo HTML generated yet for this block.</div>`) +
      `</section>\n`;
  });

  const mdSections = markdown ? formatMarkdownToDA(markdown) : "";

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>${escapeAttr(pageTitle)} | Edge Delivery Services</title>
<style>
  * { box-sizing: border-box; }
  body { margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; color:#334155; background:#f1f5f9; line-height:1.65; }
  .page-banner { background:#0f172a; color:#fff; padding:14px 28px; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; box-shadow:0 2px 10px rgba(0,0,0,0.15); }
  .page-banner .brand { display:flex; align-items:center; gap:10px; font-weight:700; font-size:0.95rem; }
  .page-banner .badge { background:#f97316; color:#fff; font-size:0.72rem; padding:3px 8px; border-radius:4px; font-weight:800; text-transform:uppercase; }
  .page-banner .source-tag { font-size:0.8rem; color:#94a3b8; font-family:ui-monospace,SFMono-Regular,Menlo,monospace; background:#1e293b; padding:4px 10px; border-radius:4px; }
  .container { max-width:1100px; margin:28px auto 80px; padding:0 24px; }
  .page-headline { margin:0 0 6px; font-size:2.1rem; font-weight:800; color:#0f172a; line-height:1.15; }
  .root-ref { margin:0 0 24px; font-size:0.85rem; color:#64748b; }
  .root-ref code { background:#e2e8f0; padding:2px 8px; border-radius:4px; font-size:0.85rem; color:#0f172a; }
  .page-block-section { background:#fff; border:1px solid #e2e8f0; border-radius:14px; padding:24px; margin-bottom:24px; box-shadow:0 1px 3px rgba(0,0,0,0.06); }
  .page-block-tag { font-size:0.72rem; font-weight:800; text-transform:uppercase; letter-spacing:0.05em; color:#0284c7; background:#e0f2fe; display:inline-block; padding:3px 10px; border-radius:4px; margin-bottom:16px; }
  .page-block-empty { color:#94a3b8; font-size:0.9rem; text-align:center; padding:20px; }
  .md-content { background:#fff; border:1px solid #e2e8f0; border-radius:14px; padding:28px; }
</style>
${blockStyles}
</head>
<body>
  <header class="page-banner">
    <div class="brand"><span>📄 ${escapeAttr(pageTitle)}</span><span class="badge">Edge Delivery Services</span></div>
    <div class="source-tag">JCR Source: ${escapeAttr(pagePath || "n/a")}</div>
  </header>
  <div class="container">
    <h1 class="page-headline">${escapeAttr(pageTitle)}</h1>
    <div class="root-ref">Root path reference: <code>${escapeAttr(pagePath || "n/a")}</code> — ${blocks.length} block(s) authored on this page</div>
    ${blockSections}
    ${mdSections ? `<div class="md-content">${mdSections}</div>` : ""}
  </div>
</body>
</html>`;
}

function formatMarkdownToDA(markdown) {
  if (!markdown) return '<div style="text-align:center;color:#64748b;">No content available.</div>';

  const lines = markdown.split('\n');
  let html = '';
  let inTable = false;
  let tableRows = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    if (line.startsWith('|')) {
      inTable = true;
      const cols = line.split('|').map(c => c.trim()).filter((c, idx, arr) => idx > 0 && idx < arr.length - 1);
      tableRows.push(cols);
      continue;
    } else {
      if (inTable) {
        html += renderDATable(tableRows);
        inTable = false;
        tableRows = [];
      }
    }

    if (line.startsWith('# ')) {
      html += `<h1 style="font-size:1.8rem; font-weight:800; border-bottom:2px solid #e2e8f0; padding-bottom:8px; margin-top:20px; margin-bottom:12px; color:#0f172a;">${line.substring(2)}</h1>`;
    } else if (line.startsWith('## ')) {
      html += `<h2 style="font-size:1.4rem; font-weight:700; margin-top:16px; margin-bottom:10px; color:#1e293b;">${line.substring(3)}</h2>`;
    } else if (line.startsWith('### ')) {
      html += `<h3 style="font-size:1.15rem; font-weight:700; margin-top:14px; margin-bottom:8px; color:#334155;">${line.substring(4)}</h3>`;
    } else if (line === '---' || line === '***') {
      html += '<hr style="border:0; border-top:2px dashed #cbd5e1; margin:20px 0;">';
    } else if (line) {
      html += `<p style="font-size:0.92rem; line-height:1.6; color:#475569; margin-bottom:10px;">${line}</p>`;
    }
  }

  if (inTable) {
    html += renderDATable(tableRows);
  }

  return html;
}

function renderDATable(rows) {
  if (rows.length === 0) return '';
  if (rows.length > 1 && rows[1].every(col => col.startsWith('-') || col.endsWith('-'))) {
    rows.splice(1, 1);
  }

  let html = '<table style="width:100%; border:2px solid #2563eb; border-collapse:collapse; margin:14px 0; background:#f8fafc; font-family:var(--font-mono); font-size:0.8rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">';
  rows.forEach((row, rIdx) => {
    const isHeader = rIdx === 0;
    html += `<tr style="${isHeader ? 'background:#dbeafe; color:#1e40af; font-weight:bold; border-bottom:2px solid #2563eb;' : 'border-bottom:1px solid #cbd5e1;'}">`;
    row.forEach(col => {
      html += `<td style="padding:8px 10px; border-right:1px solid #cbd5e1;">${col}</td>`;
    });
    html += '</tr>';
  });
  html += '</table>';
  return html;
}

function renderComponentsTable(components) {
  const tbody = document.querySelector("#table-components tbody");
  if (!tbody) return;
  tbody.innerHTML =
    components && components.length > 0
      ? components
          .map(
            (c) =>
              `<tr><td><code>${c.resourceType}</code></td><td>${c.title || "-"}</td><td>${c.group || "-"}</td><td><b style="color:var(--primary);">${c.proposedEdsBlock || "-"}</b></td><td><span style="background:rgba(56,189,248,0.1); color:var(--primary); padding:3px 8px; border-radius:4px; font-size:0.75rem; font-weight:700;">${c.capabilityClassification || "SUPPORTED"}</span></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="5">No components analyzed yet.</td></tr>';
}

function renderRedirectsTable(list) {
  const tbody = document.querySelector("#table-redirects tbody");
  if (!tbody) return;
  tbody.innerHTML =
    list && list.length > 0
      ? list
          .map(
            (r) =>
              `<tr><td><code>${r.sourceUrl}</code></td><td><code>${r.targetUrl}</code></td><td><span style="color:var(--accent); font-weight:700;">${r.statusCode || 301}</span></td><td>${r.conflict ? '<span style="color:var(--warn);">⚠️ Conflict</span>' : '<span style="color:var(--accent);">OK</span>'}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="4">No redirects mapped.</td></tr>';
}

function renderDependenciesTable(list) {
  const tbody = document.querySelector("#table-dependencies tbody");
  if (!tbody) return;
  tbody.innerHTML =
    list && list.length > 0
      ? list
          .map(
            (d) =>
              `<tr><td><code>${d.source}</code></td><td><code>${d.target}</code></td><td><span style="color:var(--primary);">${d.edgeType}</span></td><td>${d.impactLevel || "LOW"}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="4">No dependencies computed.</td></tr>';
}

function renderRolloutTable(list) {
  const tbody = document.querySelector("#table-rollout tbody");
  if (!tbody) return;
  tbody.innerHTML =
    list && list.length > 0
      ? list
          .map(
            (s) =>
              `<tr><td>#${s.stageIndex}</td><td><b>${s.stageName}</b></td><td><span style="color:var(--accent); font-weight:700;">${s.targetTrafficPercent}%</span></td><td>${s.status}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="4">No rollout stages initialized.</td></tr>';
}

function renderRepairsTable(list) {
  const tbody = document.querySelector("#table-repairs tbody");
  if (!tbody) return;
  tbody.innerHTML =
    list && list.length > 0
      ? list
          .map(
            (r) =>
              `<tr><td><code>${r.targetPath}</code></td><td>#${r.attemptNumber}</td><td>${r.issueCategory || "STYLE"}</td><td>${r.successful ? '<span style="color:var(--accent);">✅ Fixed</span>' : '<span style="color:var(--danger);">❌ Failed</span>'}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="4">No repair attempts recorded.</td></tr>';
}

function renderBenchmarksTable(list) {
  const tbody = document.querySelector("#table-benchmarks tbody");
  if (!tbody) return;
  tbody.innerHTML =
    list && list.length > 0
      ? list
          .map(
            (b) =>
              `<tr><td><b>${b.agent}</b></td><td>${b.operation}</td><td>${b.durationMs}ms</td><td>${(b.costMicros || 0).toFixed(1)}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="4">No benchmark samples recorded.</td></tr>';
}

// ─────────────────────────────────────────────────────────────
// Realtime Agent Chat
// ─────────────────────────────────────────────────────────────
let chatHistoryLoaded = false;
let chatHistory = []; // [{role:'user'|'agent', text}] for conversational memory

const CHAT_COMMANDS = [
  { re: /run (a )?dry[ -]?run/i,     action: () => { showTab("overview"); runDryRun();      return "🔍 Starting dry run for project `" + currentProjectId + "` — watch the pipeline stepper."; } },
  { re: /(generate blocks|run migration|generate (the )?blocks)/i, action: () => { showTab("components"); runMigration();  return "⚡ Generating blocks for project `" + currentProjectId + "`."; } },
  { re: /(commit|push).*(git|github)|publish/i, action: () => { showTab("overview"); runPushToGit();    return "🚀 Committing and pushing blocks to Git."; } },
  { re: /show (me )?(the )?(live )?events/i,    action: () => { showTab("overview");     refreshDashboard(); return "📡 Opened the Overview tab containing the Live Events Stream."; } },
  { re: /show (me )?(the )?(generated )?blocks/i, action: () => { showTab("components"); refreshDashboard(); return "📦 Opened the Generated Blocks tab and refreshed the data."; } },
  { re: /show (me )?(the )?(pages|scope|discovered)/i, action: () => { showTab("pages"); refreshDashboard(); return "📄 Opened the Pages & Scope tab."; } },
  { re: /show (me )?(the )?estimate|cost/i,     action: () => { showTab("estimate"); refreshDashboard(); return "💰 Opened the Estimate & Cost tab."; } },
  { re: /^refresh( dashboard)?$/i,   action: () => { refreshDashboard(); return "🔄 Dashboard refreshed."; } },
];

function tryChatCommand(msg) {
  for (const cmd of CHAT_COMMANDS) {
    if (cmd.re.test(msg)) {
      let reply;
      try { reply = cmd.action(); } catch (e) { reply = "⚠️ Could not run that action: " + e.message; }
      return reply;
    }
  }
  return null;
}

function quickChat(text) {
  const input = document.getElementById("chat-input");
  if (!input) return;
  input.value = text;
  sendChat();
}

function formatMarkdown(text) {
  if (!text) return "";
  let html = text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  // Code blocks: ```javascript ... ```
  html = html.replace(/```(?:[a-zA-Z0-9]+)?([\s\S]*?)```/g, (match, code) => {
    return `<pre class="chat-code"><code>${code.trim()}</code></pre>`;
  });

  // Inline code: `code`
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

  // Bold: **text**
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

  // Bullet lists: - item or * item
  html = html.replace(/^\s*[-*]\s+(.+)$/gm, '<li>$1</li>');

  // Wrap list items in ul
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>');

  // Clean up duplicate consecutive ul elements
  html = html.replace(/<\/ul>\s*<ul>/g, '');

  // Paragraphs / line breaks (only when not inside ul/pre/code tags)
  html = html.split('\n').map(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith('<pre') || trimmed.startsWith('<ul') || trimmed.startsWith('<li') || trimmed.startsWith('</ul') || trimmed.startsWith('</li') || trimmed.startsWith('<code>') || trimmed.startsWith('</pre>') || trimmed.startsWith('</ul>')) {
      return line;
    }
    return line ? `<p>${line}</p>` : '';
  }).join('\n');

  return html;
}

function appendChatMessage(role, text) {
  const wrap = document.getElementById("chat-messages");
  if (!wrap) return;
  const div = document.createElement("div");
  div.className = "chat-msg " + (role === "user" ? "chat-msg-user" : "chat-msg-agent");
  const bubble = document.createElement("div");
  bubble.className = "chat-bubble";
  if (role === "user") {
    bubble.textContent = text;
  } else {
    bubble.innerHTML = formatMarkdown(text);
  }
  div.appendChild(bubble);
  wrap.appendChild(div);
  wrap.scrollTop = wrap.scrollHeight;
}

async function loadChatHistory() {
  if (chatHistoryLoaded || !currentProjectId) return;
  try {
    const events = await api("/projects/" + encodeURIComponent(currentProjectId) + "/events");
    if (Array.isArray(events)) {
      const chatEvents = events.filter((e) => e.agent === "chat-user" || e.agent === "chat-agent");
      if (chatEvents.length > 0) {
        document.getElementById("chat-messages").innerHTML = "";
        chatEvents.forEach((e) => {
          appendChatMessage(e.agent === "chat-user" ? "user" : "agent", e.message || "");
          chatHistory.push({ role: e.agent === "chat-user" ? "user" : "agent", text: e.message || "" });
        });
      }
    }
    chatHistoryLoaded = true;
  } catch (e) {
    console.log("Chat history:", e);
  }
}

async function sendChat() {
  const input = document.getElementById("chat-input");
  const btn = document.getElementById("chat-send-btn");
  const msg = (input.value || "").trim();
  if (!msg) return;
  input.value = "";
  appendChatMessage("user", msg);
  chatHistory.push({ role: "user", text: msg });

  // Local interactive commands — no round trip needed
  const commandReply = tryChatCommand(msg);
  if (commandReply) {
    appendChatMessage("agent", commandReply);
    chatHistory.push({ role: "agent", text: commandReply });
    return;
  }

  btn.disabled = true;
  btn.innerText = "⏳ Thinking...";
  const typing = document.createElement("div");
  typing.className = "chat-msg chat-msg-agent chat-typing";
  typing.innerHTML = '<div class="chat-bubble">Agent is typing…</div>';
  document.getElementById("chat-messages").appendChild(typing);
  try {
    const res = await api("/projects/" + encodeURIComponent(currentProjectId) + "/chat", {
      method: "POST",
      body: JSON.stringify({ message: msg, history: chatHistory.slice(-10) }),
    });
    typing.remove();
    appendChatMessage("agent", res.reply || "(no response)");
    chatHistory.push({ role: "agent", text: res.reply || "" });
  } catch (e) {
    typing.remove();
    const detail = e && e.message ? e.message : String(e);
    appendChatMessage("agent", "⚠️ Error talking to the agent: " + (detail || "unknown error — check browser console & AEM logs"));
    console.error("Chat error:", e);
  } finally {
    btn.disabled = false;
    btn.innerText = "Send ➤";
    input.focus();
  }
}

window.addEventListener("load", async () => {
  try {
    await loadProjectsList();
    if (projectsList && projectsList.length > 0) {
      currentProjectId = projectsList[0].id;
      await populateFormFromProject(currentProjectId);
      await refreshDashboard();
    }
  } catch (e) {
    console.log("Dashboard init:", e);
  }
});
