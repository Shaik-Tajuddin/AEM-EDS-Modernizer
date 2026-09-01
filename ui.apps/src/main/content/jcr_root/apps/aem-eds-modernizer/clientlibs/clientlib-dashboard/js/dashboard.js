/**
 * AEM EDS Modernizer dashboard client.
 * Wrapped in an IIFE so no function or state leaks into the global scope;
 * everything the markup needs is exported on window.AemEdsDashboard at the bottom.
 */
(function () {
  "use strict";

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
  const errText = await res.text();
  let data = {};
  try {
    data = errText ? JSON.parse(errText) : {};
  } catch (parseErr) {
    throw new Error(`HTTP ${res.status}: ${(errText || "").substring(0, 200)}`);
  }
  if (!res.ok) {
    throw new Error(
      data.error || `HTTP ${res.status}: ${(errText || "").substring(0, 200)}`,
    );
  }
  return data;
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
  if (tabId === "github") {
    const input = document.getElementById("github-branch-input");
    if (input && !input.value) input.value = `feat/${currentProjectId}`;
    const branchDisplay = document.getElementById("vscode-branch-display");
    if (branchDisplay && input)
      branchDisplay.innerText = input.value || `feat/${currentProjectId}`;
    const newTabBtn = document.getElementById("btn-open-vscode-newtab");
    if (newTabBtn) {
      newTabBtn.href = getVsCodeUrlForBranch(
        input ? input.value : `feat/${currentProjectId}`,
      );
      newTabBtn.style.display = "inline-flex";
    }
  }
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

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function log(agent, msg) {
  const term = document.getElementById("terminal");
  const time = new Date().toLocaleTimeString();
  const line = `<div class="log-line"><span class="log-time">[${escapeHtml(time)}]</span><span class="log-agent">[${escapeHtml(agent)}]</span><span>${escapeHtml(msg)}</span></div>`;
  if (term) {
    term.innerHTML += line;
    term.scrollTop = term.scrollHeight;
  }
}

function onProviderChange() {
  const provider = document.getElementById("cfg-aiProvider").value;
  const modelInput = document.getElementById("cfg-aiModel");
  const banner = document.getElementById("ide-agent-banner");
  const bannerTitle = document.getElementById("ide-agent-banner-title");
  const cloudBanner = document.getElementById("cloud-api-banner");
  const cloudTitle = document.getElementById("cloud-api-banner-title");
  const cloudKeyRef = document.getElementById("cloud-api-key-ref");
  const modelGroup = document.getElementById("ai-model-group");
  const budgetGroup = document.getElementById("ai-budget-group");

  const ideProviders = ["antigravity", "cursor", "claudecode", "geminicode"];
  const cloudKeyRefs = {
    tokenrouter: "Configured via OSGi (sk-qPQo0Pl4HEhffxvWVdsiGVN0cPIxQvoeHcF5aQnGMcYLa11f)",
    anthropic: "env:ANTHROPIC_API_KEY",
    openai: "env:OPENAI_API_KEY",
    gemini: "env:GEMINI_API_KEY",
  };
  const isIde = ideProviders.indexOf(provider) >= 0;
  const isCloudApi = Object.prototype.hasOwnProperty.call(
    cloudKeyRefs,
    provider,
  );
  if (banner) banner.style.display = isIde ? "block" : "none";
  if (bannerTitle && isIde) {
    const labels = {
      antigravity: "Antigravity",
      cursor: "Cursor",
      claudecode: "Claude Code",
      geminicode: "Gemini IDE",
    };
    bannerTitle.textContent = (labels[provider] || "IDE") + " Mode Active";
  }
  if (cloudBanner) cloudBanner.style.display = isCloudApi ? "block" : "none";
  if (isCloudApi) {
    const cloudLabels = {
      tokenrouter: "TokenRouter API",
      anthropic: "Anthropic Claude",
      openai: "OpenAI GPT",
      gemini: "Google Gemini",
    };
    if (cloudTitle)
      cloudTitle.textContent =
        (cloudLabels[provider] || "Cloud API") + " — API key configured";
    if (cloudKeyRef) cloudKeyRef.textContent = cloudKeyRefs[provider];
  }
  // Local IDE: keep model visible for Ollama chat model; budget still hidden
  if (modelGroup) modelGroup.style.display = "";
  if (budgetGroup) budgetGroup.style.display = isIde ? "none" : "";
  if (modelInput) {
    if (isIde) {
      if (
        !modelInput.value ||
        modelInput.value.indexOf("claude") === 0 ||
        modelInput.value.indexOf("gpt") === 0 ||
        modelInput.value.indexOf("gemini-1") === 0 ||
        modelInput.value.indexOf("glm") === 0
      ) {
        modelInput.value = "qwen3:8b";
      }
      modelInput.placeholder = "Ollama model (e.g. qwen3:8b)";
    } else if (provider === "tokenrouter") {
      modelInput.value = "z-ai/glm-5.3-free";
      modelInput.placeholder = "z-ai/glm-5.3-free";
    } else if (provider === "anthropic") {
      modelInput.value = "claude-3-5-sonnet-20241022";
      modelInput.placeholder = "model identifier";
    } else if (provider === "openai") {
      modelInput.value = "gpt-4o";
      modelInput.placeholder = "model identifier";
    } else if (provider === "gemini") {
      modelInput.value = "gemini-1.5-pro";
      modelInput.placeholder = "model identifier";
    } else if (provider === "ollama") {
      modelInput.value = "qwen3:8b";
      modelInput.placeholder = "Ollama model";
    }
  }
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
  const scopeMode = document.querySelector(
    'input[name="cfg-scopeMode"]:checked',
  )
    ? document.querySelector('input[name="cfg-scopeMode"]:checked').value
    : "RECURSIVE";

  const payload = {
    id: document.getElementById("cfg-id").value.trim() || "project-1",
    name:
      document.getElementById("cfg-name").value.trim() || "Untitled Project",
    aemAuthorUrl: document.getElementById("cfg-authorUrl").value.trim(),
    aemPublishUrl: document.getElementById("cfg-publishUrl").value.trim(),
    contentRoot: document.getElementById("cfg-contentRoot").value.trim(),
    pageScope: document.getElementById("cfg-pageScope").value.trim(),
    scopeMode: scopeMode,
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
      `Project '${saved.name}' saved. Scope: ${saved.pageScope || "all"} (${saved.scopeMode || "RECURSIVE"})`,
    );
    showToast(
      `Project configuration saved! (${scopeMode === "SINGLE_PAGE" ? "This Page Only" : "Complete Subtree"})`,
    );
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
  const label = p ? p.name || p.id : currentProjectId;
  if (
    !confirm(
      `Delete project "${label}"?\n\nThis removes its saved config, jobs, inventory and generated blocks. This cannot be undone.`,
    )
  )
    return;

  try {
    await api(`projects/${encodeURIComponent(currentProjectId)}/delete`, {
      method: "POST",
    });
    showToast(`🗑️ Project '${label}' deleted`);
    projectsList = projectsList.filter((x) => x.id !== currentProjectId);
    currentProjectId =
      projectsList && projectsList.length > 0
        ? projectsList[0].id
        : "wknd-site";
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
    resetVsCodeReviewGate();
    await populateFormFromProject(val);
    await refreshDashboard();
  }
}

async function onQuickScopeChange(mode) {
  const radRec = document.getElementById("cfg-scopeMode-recursive");
  const radSingle = document.getElementById("cfg-scopeMode-single");
  if (radRec && radSingle) {
    radRec.checked = mode !== "SINGLE_PAGE";
    radSingle.checked = mode === "SINGLE_PAGE";
  }

  const modeLabel =
    mode === "SINGLE_PAGE" ? "This Page Only" : "Complete Nested Subtree";
  showToast(`Discovery scope set to: ${modeLabel}`);

  try {
    const p =
      projectsList.find((x) => x.id === currentProjectId) ||
      (await api(`projects/${currentProjectId}`));
    if (p) {
      p.scopeMode = mode;
      await api("projects", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(p),
      });
      log(
        "connection",
        `Updated crawl scope to: ${modeLabel}. Click 'Run Dry Run' to apply.`,
      );
    }
  } catch (e) {
    console.warn("Could not save scope change:", e);
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

    const scopeMode = p.scopeMode || "RECURSIVE";
    const radRec = document.getElementById("cfg-scopeMode-recursive");
    const radSingle = document.getElementById("cfg-scopeMode-single");
    if (radRec && radSingle) {
      radRec.checked = scopeMode !== "SINGLE_PAGE";
      radSingle.checked = scopeMode === "SINGLE_PAGE";
    }

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
    onProviderChange();

    const authorUrl = (p.aemAuthorUrl || "http://localhost:4502").replace(/\/$/, "");
    const rootPath = (p.contentRoot || "/content/wknd").trim();
    const cleanRootPath = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
    const fullAuthoredUrl = authorUrl + cleanRootPath + (cleanRootPath.endsWith(".html") ? "" : ".html");
    const rootpathDisplay = document.getElementById("devserver-rootpath-display");
    if (rootpathDisplay) {
      rootpathDisplay.textContent = fullAuthoredUrl;
    }
  }
}

let generatedFiles = [];
let blockFilesMap = {};
let activeBlockName = null;
let activeFileTab = "demo";
let vscodeReviewConfirmed = false;

function resetVsCodeReviewGate() {
  vscodeReviewConfirmed = false;
  const chk = document.getElementById("chk-vscode-reviewed");
  if (chk) chk.checked = false;
  const btnCreatePr = document.getElementById("btn-create-pr") || document.getElementById("btn-publish");
  if (btnCreatePr) btnCreatePr.disabled = true;
}

function onVsCodeReviewToggle() {
  const chk = document.getElementById("chk-vscode-reviewed");
  vscodeReviewConfirmed = !!(chk && chk.checked);
  const btnCreatePr = document.getElementById("btn-create-pr") || document.getElementById("btn-publish");
  if (btnCreatePr) btnCreatePr.disabled = !vscodeReviewConfirmed;
  if (vscodeReviewConfirmed) {
    setPipelineStep("vscode", "done");
    setPipelineStep("publish", "active");
  }
}

function featureBranchName() {
  return "feat/" + currentProjectId;
}

function applyPreviewBranch(branch) {
  const name = branch || featureBranchName();
  const input = document.getElementById("github-branch-input");
  if (input) input.value = name;
  const branchDisplay = document.getElementById("vscode-branch-display");
  if (branchDisplay) branchDisplay.innerText = name;
}

async function previewToBranch(paths) {
  resetVsCodeReviewGate();
  setPipelineStep("validate", "done");
  setPipelineStep("vscode", "active");
  applyPreviewBranch(featureBranchName());
  const isSelective = paths && Array.isArray(paths) && paths.length > 0;
  log(
    "preview",
    isSelective
      ? `Committing and pushing ${paths.length} selected files to branch '${featureBranchName()}'...`
      : `Committing and pushing all generated files to branch '${featureBranchName()}' (no PR)...`,
  );
  const response = await api(`projects/${currentProjectId}/preview`, {
    method: "POST",
    body: isSelective ? JSON.stringify({ paths, branch: featureBranchName() }) : undefined,
  });
  // New envelope: { job, healing, prReady }; legacy: bare job
  const job = response.job || response;
  const healing = response.healing || {};
  const prReady = response.prReady !== undefined
    ? !!response.prReady
    : !!healing.ok;
  setHealingBadges(healing, prReady);
  const meta = job.metadata || {};
  const branch = meta.branch || featureBranchName();
  applyPreviewBranch(branch);
  const vscodeUrl = meta.vscodeUrl || getVsCodeUrlForBranch(branch);
  showTab("github");
  loadVsCodeFrame(vscodeUrl);
  setPipelineStep("vscode", "done");
  setPipelineStep("publish", "active");
  if (healing.prunedBlocks && healing.prunedBlocks.length) {
    log("preview", `Deduplicated blocks removed: ${healing.prunedBlocks.join(", ")}`);
  }
  log(
    "preview",
    `Branch '${branch}' is ready.${prReady
      ? " Pre-PR healing passed — Create PR is enabled."
      : " Healing incomplete — Create PR remains locked until lint/build/push pass."}`,
  );
  showToast(
    prReady
      ? "Branch pushed & healing passed. You can open the PR."
      : "Branch pushed, but healing failed — PR gate locked.",
  );
  try {
    await checkBranchStatus();
  } catch (e) {
    /* optional */
  }
  return job;
}

function setHealingBadges(healing, prReady) {
  const repoBadge = document.getElementById("badge-repo-status");
  const healBadge = document.getElementById("badge-healing-status");
  const gateBadge = document.getElementById("badge-pr-gate");
  if (repoBadge) repoBadge.innerText = `● Local repo: ${healing.checkout === "OK" ? "ready" : "unknown"}`;
  if (healBadge) {
    healBadge.innerText = `● Healing: ${healing.ok ? "passed ✓" : "failed / not run"}`;
    healBadge.style.color = healing.ok ? "#22c55e" : "#f59e0b";
  }
  if (gateBadge) {
    gateBadge.innerText = prReady ? "🔓 PR gate: open" : "🔒 PR gate: locked";
    gateBadge.style.color = prReady ? "#22c55e" : "#94a3b8";
  }
  const btnPublish = document.getElementById("btn-publish");
  if (btnPublish && !prReady) btnPublish.disabled = true;
}

async function aemUpControl(action) {
  log("devserver", `Dev server action: ${action}...`);
  try {
    const result = await api(`projects/${currentProjectId}/aem-up`, {
      method: "POST",
      body: JSON.stringify({ action }),
    });
    const badge = document.getElementById("badge-aemup-status");
    const running = result.running === true || result.status === "RUNNING" || result.status === "STARTED";
    if (badge) {
      badge.innerText = `● Dev server: ${running ? "running" : "stopped"}`;
      badge.style.color = running ? "#22c55e" : "#94a3b8";
    }
    log("devserver", `aem up ${action}: ${result.status || "ok"} → ${result.url || ""}`);
    showToast(running ? "Dev server running at http://localhost:3000" : `Dev server ${result.status || "updated"}.`);
    return result;
  } catch (err) {
    log("error", `Dev server ${action} failed: ${err.message}`);
    showToast(`Dev server ${action} failed: ${err.message}`);
  }
}

async function runAiCompare() {
  const btn = document.getElementById("btn-ai-compare");
  const resultEl = document.getElementById("ai-compare-result");
  const edsPath = document.getElementById("ai-compare-eds-path");
  const blockName = document.getElementById("ai-compare-block");
  const aemPagePath = activePagePath || (currentProject && currentProject.contentRoot) || "/content/wknd";
  if (btn) btn.disabled = true;
  if (resultEl) resultEl.innerText = "Comparing AEM source page with local EDS render...";
  try {
    const report = await api(`projects/${currentProjectId}/compare`, {
      method: "POST",
      body: JSON.stringify({
        aemPagePath,
        edsPagePath: edsPath ? edsPath.value.trim() : "",
        blockName: blockName ? blockName.value.trim() || null : null,
      }),
    });
    if (resultEl) {
      const files = (report.updatedFiles || []).join(", ") || "none";
      resultEl.innerHTML =
        `<b>Status:</b> ${report.status} · AEM fetched: ${report.aemFetched ? "✓" : "✗"} · EDS fetched: ${report.edsFetched ? "✓" : "✗"}<br>` +
        `<b>Files updated:</b> ${files}` +
        (report.analysis ? `<br><details><summary>AI analysis</summary><pre style="white-space:pre-wrap; max-height:200px; overflow:auto;">${escapeHtmlBody(report.analysis)}</pre></details>` : "");
    }
    log("ai-compare", `AI comparison finished: ${report.status}. Updated: ${report.updatedFiles?.join(", ") || "none"}`);
    showToast(`AI comparison: ${report.status}.`);
    await refreshDashboard();
  } catch (err) {
    if (resultEl) resultEl.innerText = `Comparison failed: ${err.message}`;
    log("error", `AI comparison failed: ${err.message}`);
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function runAiCompareFromDevServer() {
  const urlInput = document.getElementById("cfg-devserver-page-url");
  const resultEl = document.getElementById("devserver-compare-result");
  const targetUrl = urlInput ? urlInput.value.trim() : "";
  const rootDisplay = document.getElementById("devserver-rootpath-display");
  const aemPagePath = (rootDisplay && rootDisplay.textContent) ? rootDisplay.textContent.trim() : "/content/wknd";
  if (resultEl) resultEl.innerText = "Comparing authored page with AEM rootpath reference...";
  try {
    const report = await api(`projects/${currentProjectId}/compare`, {
      method: "POST",
      body: JSON.stringify({
        aemPagePath,
        edsPagePath: targetUrl || "http://localhost:3000",
        blockName: null,
      }),
    });
    if (resultEl) {
      const files = (report.updatedFiles || []).join(", ") || "none";
      resultEl.innerHTML =
        `<b>Status:</b> ${report.status} · AEM: ${report.aemFetched ? "✓" : "✗"} · EDS: ${report.edsFetched ? "✓" : "✗"}<br>` +
        `<b>Updated:</b> ${files}` +
        (report.analysis ? `<br><details style="margin-top:4px;"><summary style="cursor:pointer; font-size:0.74rem;">AI analysis</summary><pre style="white-space:pre-wrap; max-height:160px; overflow:auto; font-size:0.72rem; margin-top:4px;">${escapeHtmlBody(report.analysis)}</pre></details>` : "");
    }
    log("ai-compare", `DevServer AI comparison finished: ${report.status}. Updated: ${report.updatedFiles?.join(", ") || "none"}`);
    showToast(`AI comparison: ${report.status}`);
    await refreshDashboard();
  } catch (err) {
    if (resultEl) resultEl.innerText = `Comparison failed: ${err.message}`;
    log("error", `DevServer AI comparison failed: ${err.message}`);
  }
}

function escapeHtmlBody(text) {
  return String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

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
    const response = await api(`projects/${currentProjectId}/dryrun`, {
      method: "POST",
    });
    const job = response.job || response;
    const localRepo = response.localRepo || {};
    log(
      "localrepo",
      `Local EDS repo: ${localRepo.status || "unknown"} at ${localRepo.path || "—"} (npm install: ${localRepo.npmInstall || "n/a"})`,
    );
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
    setPipelineStep("validate", "done");
    await refreshDashboard();

    showTab("components");
    applyPreviewBranch(featureBranchName());
    showToast("Blocks generated successfully! Review in Components Inspector or go to VS Code & GitHub to commit.");
  } catch (err) {
    log("error", `Generation failed: ${err.message}`);
  } finally {
    if (btnMigrate) btnMigrate.disabled = false;
  }
}

async function runPushToGit() {
  if (!vscodeReviewConfirmed) {
    showToast("Confirm you have reviewed the branch in VS Code first.");
    showTab("github");
    return;
  }
  const btnPublish = document.getElementById("btn-publish") || document.getElementById("btn-create-pr");
  if (btnPublish) { btnPublish.disabled = true; btnPublish.textContent = "Opening PR…"; }
  setPipelineStep("vscode", "done");
  setPipelineStep("publish", "active");
  log(
    "publishing",
    `Opening Pull Request from '${featureBranchName()}' (no re-commit of generated files)...`,
  );
  try {
    const job = await api(`projects/${currentProjectId}/publish`, {
      method: "POST",
    });
    const prUrl = (job.metadata && job.metadata.prUrl) || job.prUrl;
    log(
      "publishing",
      prUrl
        ? `Pull Request ready: ${prUrl}`
        : `Publish job finished with state: ${job.state}`,
    );
    setPipelineStep("publish", "done");
    await refreshDashboard();
    const resultEl = document.getElementById("github-pr-result");
    if (resultEl && prUrl) {
      resultEl.innerHTML = `<div style="background:rgba(34,197,94,0.1); border:1px solid rgba(34,197,94,0.3); border-radius:6px; padding:10px 14px; margin-top:8px;"><span style="color:#22c55e; font-weight:700;">✅ Pull Request Ready:</span> <a href="${escapeAttr(prUrl)}" target="_blank" style="color:var(--accent); font-weight:700; text-decoration:underline; margin-left:6px;">${escapeHtml(prUrl)}</a></div>`;
    }
    showToast(
      prUrl
        ? "Pull Request ready: " + prUrl
        : "Publish job completed.",
    );
  } catch (err) {
    log("error", `Git PR failed: ${err.message}`);
    showToast("Error opening PR: " + err.message);
  } finally {
    if (btnPublish) {
      btnPublish.disabled = !vscodeReviewConfirmed;
      btnPublish.textContent = "📤 Open Pull Request";
    }
  }
}

function encodeBranchSegments(branch) {
  return String(branch || "main")
    .split("/")
    .map(encodeURIComponent)
    .join("/");
}

function getVsCodeUrlForBranch(branch) {
  const repoInput = document.getElementById("cfg-repoUrl");
  let repoUrl = repoInput ? repoInput.value.trim() : "";
  if (!repoUrl) {
    const activePrj = projectsList.find((p) => p.id === currentProjectId);
    if (activePrj && activePrj.edsGitRepoUrl) {
      repoUrl = activePrj.edsGitRepoUrl;
    }
  }
  if (!repoUrl) repoUrl = "https://github.com/my-org/wknd-eds";

  let cleaned = repoUrl.trim();
  if (cleaned.endsWith(".git"))
    cleaned = cleaned.substring(0, cleaned.length - 4);
  if (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length - 1);
  if (cleaned.startsWith("https://github.com/")) {
    const ownerRepo = cleaned.substring("https://github.com/".length);
    return `https://vscode.dev/github/${ownerRepo}/tree/${encodeBranchSegments(branch || "feat/" + currentProjectId)}`;
  }
  return "https://vscode.dev";
}

let workspaceOpenPath = "";

function syncWsLineNumbers() {
  const editor = document.getElementById("ws-editor");
  const gutter = document.getElementById("ws-line-numbers");
  if (!editor || !gutter) return;
  const text = editor.value || "";
  const count = text.split(/\r\n|\r|\n/).length;
  let lines = "";
  for (let i = 1; i <= count; i++) {
    lines += i + (i === count ? "" : "\n");
  }
  gutter.textContent = lines || "1";
  syncWsLineScroll();
}

function syncWsLineScroll() {
  const editor = document.getElementById("ws-editor");
  const gutter = document.getElementById("ws-line-numbers");
  if (editor && gutter) {
    gutter.scrollTop = editor.scrollTop;
  }
}

function loadVsCodeFrame(customUrl) {
  const input = document.getElementById("github-branch-input");
  const branch = (input && input.value.trim()) || `feat/${currentProjectId}`;
  const url = customUrl || getVsCodeUrlForBranch(branch);
  const placeholder = document.getElementById("vscode-placeholder");
  const workspace = document.getElementById("vscode-workspace");
  const newTabBtn = document.getElementById("btn-open-vscode-newtab");
  const branchDisplay = document.getElementById("vscode-branch-display");
  const hint = document.getElementById("vscode-frame-hint");

  if (branchDisplay) branchDisplay.innerText = branch;
  if (newTabBtn) {
    newTabBtn.href = url;
    newTabBtn.style.display = "inline-flex";
  }
  if (hint) {
    hint.textContent =
      "In-dashboard editor (vscode.dev cannot be embedded in AEM)";
  }
  if (placeholder) placeholder.style.display = "none";
  if (workspace) workspace.style.display = "flex";
  initWorkspaceResizer();
  loadWorkspaceFiles(branch);
}

function initWorkspaceResizer() {
  const resizer = document.getElementById("ws-resizer");
  const sidebar = document.getElementById("ws-sidebar");
  if (!resizer || !sidebar || resizer.dataset.bound) return;
  resizer.dataset.bound = "1";
  let isDragging = false;
  resizer.addEventListener("mousedown", (e) => {
    isDragging = true;
    resizer.classList.add("resizing");
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
  });
  window.addEventListener("mousemove", (e) => {
    if (!isDragging) return;
    const container = document.getElementById("vscode-frame-container");
    if (!container) return;
    const rect = container.getBoundingClientRect();
    const newWidth = Math.max(220, Math.min(650, e.clientX - rect.left));
    sidebar.style.width = newWidth + "px";
  });
  window.addEventListener("mouseup", () => {
    if (isDragging) {
      isDragging = false;
      resizer.classList.remove("resizing");
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
    }
  });
}

let currentWsViewMode = "diff";
let workspaceCurrentBaseContent = null;

function setWsViewMode(mode) {
  currentWsViewMode = mode;
  const diffPane = document.getElementById("ws-diff-pane");
  const editPane = document.getElementById("ws-editor-pane");
  const btnDiff = document.getElementById("btn-ws-mode-diff");
  const btnEdit = document.getElementById("btn-ws-mode-edit");
  const btnSave = document.getElementById("btn-ws-save-open");
  const editor = document.getElementById("ws-editor");

  if (mode === "diff") {
    if (editor && workspaceOpenPath) {
      renderGitDiff(workspaceCurrentBaseContent, editor.value, workspaceOpenPath);
    }
    if (diffPane) diffPane.style.display = "flex";
    if (editPane) editPane.style.display = "none";
    if (btnSave) btnSave.style.display = "none";
    if (btnDiff) { btnDiff.classList.add("active"); btnDiff.classList.remove("btn-outline"); }
    if (btnEdit) { btnEdit.classList.remove("active"); btnEdit.classList.add("btn-outline"); }
  } else {
    if (diffPane) diffPane.style.display = "none";
    if (editPane) editPane.style.display = "flex";
    if (btnSave) btnSave.style.display = "inline-flex";
    if (btnDiff) { btnDiff.classList.remove("active"); btnDiff.classList.add("btn-outline"); }
    if (btnEdit) { btnEdit.classList.add("active"); btnEdit.classList.remove("btn-outline"); }
    syncWsLineNumbers();
  }
}

function reloadVsCodeFrame() {
  loadVsCodeFrame();
  showToast("Reloaded branch workspace");
}

async function loadWorkspaceFiles(branch) {
  const tree = document.getElementById("ws-file-tree");
  const countEl = document.getElementById("ws-tree-count");
  if (!tree) return;
  tree.innerHTML = '<li class="ws-tree-empty">Loading files…</li>';
  const chkAll = document.getElementById("ws-select-all-chk");
  if (chkAll) { chkAll.checked = false; chkAll.indeterminate = false; }
  const btnDelSel = document.getElementById("btn-ws-delete-selected");
  if (btnDelSel) btnDelSel.style.display = "none";
  const countSel = document.getElementById("ws-selected-count");
  if (countSel) countSel.textContent = "0";

  try {
    const data = await api(`projects/${currentProjectId}/workspace`, {
      method: "POST",
      body: JSON.stringify({ branch }),
    });
    const files = data.files || [];
    workspaceFilesCache = files;
    if (countEl) countEl.textContent = `(${files.length})`;
    if (!files.length) {
      tree.innerHTML =
        '<li class="ws-tree-empty">No changed files yet. Push the preview branch first.</li>';
      return;
    }
    tree.innerHTML = files
      .map((f) => {
        const safe = String(f.path)
          .replace(/&/g, "&amp;")
          .replace(/</g, "&lt;")
          .replace(/"/g, "&quot;");
        const status = f.status || 'added';
        const add = f.additions != null ? f.additions : 0;
        const del = f.deletions != null ? f.deletions : 0;
        const statusColor = status === 'added' ? '#22c55e' : status === 'removed' ? '#ef4444' : '#38bdf8';
        const diffBadge = `<span class="ws-diff-stat">`
          + `<span style="color:${statusColor}; font-weight:700; text-transform:uppercase; font-size:0.68rem;">${status}</span>`
          + `<span style="color:#22c55e; font-weight:700;">+${add}</span>`
          + `<span style="color:#ef4444; font-weight:700;">-${del}</span>`
          + `</span>`;
        return `<li class="ws-file-row"><input type="checkbox" class="ws-file-chk" data-path="${safe}" style="cursor:pointer; margin:0 2px 0 2px; accent-color:var(--accent); flex-shrink:0;"><button type="button" class="ws-file-btn" data-path="${safe}"><span class="ws-file-name" title="${safe}">${safe}</span>${diffBadge}</button><button type="button" class="ws-file-del" data-path="${safe}" title="Delete ${safe} from branch">🗑️ Del</button></li>`;
      })
      .join("");
    if (!tree.dataset.bound) {
      tree.dataset.bound = "1";
      tree.addEventListener("change", (ev) => {
        if (ev.target && ev.target.classList.contains("ws-file-chk")) {
          onWorkspaceFileCheckboxChange();
        }
      });
      tree.addEventListener("click", (ev) => {
        const del = ev.target.closest(".ws-file-del");
        const open = ev.target.closest(".ws-file-btn");
        if (del && del.dataset.path) {
          ev.preventDefault();
          deleteWorkspaceFile(del.dataset.path);
          return;
        }
        if (open && open.dataset.path) {
          openWorkspaceFile(open.dataset.path);
        }
      });
    }
    const keepOpen =
      workspaceOpenPath && files.some((f) => f.path === workspaceOpenPath);
    if (keepOpen) {
      await openWorkspaceFile(workspaceOpenPath);
    } else if (files[0] && files[0].path) {
      await openWorkspaceFile(files[0].path);
    }
  } catch (err) {
    tree.innerHTML = `<li class="ws-tree-empty">${err.message}</li>`;
  }
}

let workspaceFilesCache = [];

async function openWorkspaceFile(path) {
  const input = document.getElementById("github-branch-input");
  const branch = (input && input.value.trim()) || `feat/${currentProjectId}`;
  const editor = document.getElementById("ws-editor");
  const label = document.getElementById("ws-open-path");
  const diffLabel = document.getElementById("ws-open-diff");
  workspaceOpenPath = path;
  if (label) label.textContent = path;

  // Render open file Git diff badge
  if (diffLabel) {
    const fileStat = workspaceFilesCache.find((f) => f.path === path);
    if (fileStat) {
      const status = fileStat.status || 'added';
      const add = fileStat.additions != null ? fileStat.additions : 0;
      const del = fileStat.deletions != null ? fileStat.deletions : 0;
      const col = status === 'added' ? '#22c55e' : status === 'removed' ? '#ef4444' : '#38bdf8';
      diffLabel.innerHTML = `<span class="ws-diff-stat" style="background:rgba(255,255,255,0.05); padding:3px 8px;">`
        + `<span style="color:${col}; font-weight:700; text-transform:uppercase;">${status}</span>`
        + `<span style="color:#22c55e; font-weight:700;">+${add} lines</span>`
        + `<span style="color:#ef4444; font-weight:700;">-${del} lines</span>`
        + `</span>`;
    } else {
      diffLabel.innerHTML = '';
    }
  }

  document.querySelectorAll(".ws-file-btn").forEach((btn) => {
    btn.parentElement.classList.toggle("active", btn.getAttribute("data-path") === path);
  });
  if (editor) editor.value = "Loading…";
  try {
    const data = await api(`projects/${currentProjectId}/workspace/file`, {
      method: "POST",
      body: JSON.stringify({ branch, path }),
    });
    if (editor) {
      editor.value = data.content || "";
      editor.readOnly = !!data.readOnly;
    }
    workspaceCurrentBaseContent = data.baseContent;
    renderGitDiff(data.baseContent, data.content, path);
    syncWsLineNumbers();
  } catch (err) {
    if (editor) editor.value = err.message;
    workspaceCurrentBaseContent = null;
    renderGitDiff(null, err.message, path);
    syncWsLineNumbers();
  }
}

function renderGitDiff(baseContent, newContent, filename) {
  const diffOutput = document.getElementById("ws-diff-output");
  if (!diffOutput) return;

  if (baseContent == null || baseContent === "") {
    // Newly Added File (All lines green '+')
    const lines = (newContent || "").split(/\r\n|\r|\n/);
    let html = `<div class="diff-line diff-line-hunk"><span>--- /dev/null</span><br><span>+++ b/${filename || "file"}</span></div>`;
    for (let i = 0; i < lines.length; i++) {
      const lineNum = i + 1;
      const safeText = String(lines[i] || " ").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
      html += `<div class="diff-line diff-line-added">`
        + `<div class="diff-num"></div>`
        + `<div class="diff-num">${lineNum}</div>`
        + `<div class="diff-marker">+</div>`
        + `<div class="diff-text">${safeText}</div>`
        + `</div>`;
    }
    diffOutput.innerHTML = html;
    return;
  }

  // Modified File: Compute Line-by-line Difference
  const oldLines = baseContent.split(/\r\n|\r|\n/);
  const newLines = (newContent || "").split(/\r\n|\r|\n/);
  const diff = computeLineDiff(oldLines, newLines);

  let html = `<div class="diff-line diff-line-hunk"><span>--- a/${filename || "base"}</span><br><span>+++ b/${filename || "head"}</span></div>`;
  diff.forEach((item) => {
    const safeText = String(item.text || " ").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
    if (item.type === "added") {
      html += `<div class="diff-line diff-line-added">`
        + `<div class="diff-num"></div>`
        + `<div class="diff-num">${item.newLine}</div>`
        + `<div class="diff-marker">+</div>`
        + `<div class="diff-text">${safeText}</div>`
        + `</div>`;
    } else if (item.type === "deleted") {
      html += `<div class="diff-line diff-line-deleted">`
        + `<div class="diff-num">${item.oldLine}</div>`
        + `<div class="diff-num"></div>`
        + `<div class="diff-marker">-</div>`
        + `<div class="diff-text">${safeText}</div>`
        + `</div>`;
    } else {
      html += `<div class="diff-line diff-line-unchanged">`
        + `<div class="diff-num">${item.oldLine}</div>`
        + `<div class="diff-num">${item.newLine}</div>`
        + `<div class="diff-marker"> </div>`
        + `<div class="diff-text">${safeText}</div>`
        + `</div>`;
    }
  });

  diffOutput.innerHTML = html;
}

function computeLineDiff(a, b) {
  const n = a.length;
  const m = b.length;
  // Standard dynamic programming LCS
  const dp = Array.from({ length: n + 1 }, () => new Int32Array(m + 1));
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < m; j++) {
      if (a[i] === b[j]) {
        dp[i + 1][j + 1] = dp[i][j] + 1;
      } else {
        dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
      }
    }
  }

  const result = [];
  let i = n, j = m;
  const trace = [];
  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && a[i - 1] === b[j - 1]) {
      trace.push({ type: "unchanged", text: a[i - 1], oldLine: i, newLine: j });
      i--;
      j--;
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      trace.push({ type: "added", text: b[j - 1], oldLine: null, newLine: j });
      j--;
    } else if (i > 0 && (j === 0 || dp[i][j - 1] < dp[i - 1][j])) {
      trace.push({ type: "deleted", text: a[i - 1], oldLine: i, newLine: null });
      i--;
    }
  }
  trace.reverse();
  return trace;
}

function createNewWorkspaceFilePrompt() {
  const relPath = window.prompt(
    "Enter new file path (e.g. blocks/cards/cards.js, styles/custom.css, or component-models.json):",
    "blocks/new-block/new-block.js",
  );
  if (!relPath || !relPath.trim()) return;
  const cleanPath = relPath.trim().replace(/^\/+/, "");
  if (cleanPath === "fstab.yaml") {
    showToast("fstab.yaml is not managed here.");
    return;
  }
  const exists = workspaceFilesCache.find((f) => f.path === cleanPath);
  if (exists) {
    openWorkspaceFile(cleanPath);
    showToast("Opened existing file " + cleanPath);
    return;
  }
  let starter = "";
  if (cleanPath.endsWith(".js")) {
    starter = "export default function decorate(block) {\n  // Block decoration logic\n}\n";
  } else if (cleanPath.endsWith(".css")) {
    const blockName = cleanPath.replace(/^.*\/|\.[^.]+$/g, "");
    starter = `/* Scoped styling for ${blockName} */\n.${blockName} {\n  display: block;\n}\n`;
  } else if (cleanPath.endsWith(".json")) {
    starter = "{\n  \n}\n";
  } else if (cleanPath.endsWith(".md")) {
    starter = "# New Page / Section\n\nContent goes here.\n";
  } else if (cleanPath.endsWith(".html")) {
    starter = "<div>\n  <p>Example markup</p>\n</div>\n";
  } else if (cleanPath.endsWith(".yaml") || cleanPath.endsWith(".yml")) {
    starter = "version: 1\n";
  }

  workspaceOpenPath = cleanPath;
  workspaceCurrentBaseContent = "";
  const editor = document.getElementById("ws-editor");
  const label = document.getElementById("ws-open-path");
  const diffLabel = document.getElementById("ws-open-diff");
  if (editor) editor.value = starter;
  if (label) label.textContent = cleanPath + " (New)";
  if (diffLabel) {
    diffLabel.innerHTML = '<span class="ws-diff-stat" style="color:#22c55e; font-weight:700;">NEW</span>';
  }
  setWsViewMode("edit");
  syncWsLineNumbers();
  showToast("Ready to edit " + cleanPath + ". Click 💾 Save to write to workspace.");
}

async function saveWorkspaceFile() {
  const input = document.getElementById("github-branch-input");
  const branch = (input && input.value.trim()) || `feat/${currentProjectId}`;
  const editor = document.getElementById("ws-editor");
  const saveBtn = document.getElementById("btn-ws-save-open");
  if (!workspaceOpenPath || !editor) {
    showToast("Open a file first.");
    return;
  }
  if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = "Saving…"; }
  try {
    const saved = await api(`projects/${currentProjectId}/workspace/save`, {
      method: "POST",
      body: JSON.stringify({
        branch,
        path: workspaceOpenPath,
        content: editor.value,
      }),
    });
    if (saved && typeof saved.content === "string") {
      editor.value = saved.content;
    }
    renderGitDiff(workspaceCurrentBaseContent, editor.value, workspaceOpenPath);
    syncWsLineNumbers();
    showToast("Saved " + workspaceOpenPath + " to workspace (Ready to commit)");
    log("workspace", `Saved ${workspaceOpenPath} locally in workspace`);
    loadWorkspaceFiles(branch);
  } catch (err) {
    showToast("Save failed: " + err.message);
    log("error", `Workspace save failed: ${err.message}`);
  } finally {
    if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = "💾 Save"; }
  }
}

function onWorkspaceFileCheckboxChange() {
  const checkboxes = Array.from(document.querySelectorAll(".ws-file-chk"));
  const checked = checkboxes.filter((c) => c.checked);
  const countEl = document.getElementById("ws-selected-count");
  const btnDelSel = document.getElementById("btn-ws-delete-selected");
  const chkAll = document.getElementById("ws-select-all-chk");
  if (countEl) countEl.textContent = String(checked.length);
  if (btnDelSel) {
    btnDelSel.style.display = checked.length > 0 ? "inline-flex" : "none";
  }
  if (chkAll) {
    if (checked.length === 0) {
      chkAll.checked = false;
      chkAll.indeterminate = false;
    } else if (checked.length === checkboxes.length) {
      chkAll.checked = true;
      chkAll.indeterminate = false;
    } else {
      chkAll.checked = false;
      chkAll.indeterminate = true;
    }
  }
}

function toggleSelectAllWorkspaceFiles() {
  const chkAll = document.getElementById("ws-select-all-chk");
  const shouldCheck = !!(chkAll && chkAll.checked);
  document.querySelectorAll(".ws-file-chk").forEach((c) => {
    c.checked = shouldCheck;
  });
  onWorkspaceFileCheckboxChange();
}

async function deleteSelectedWorkspaceFiles() {
  const input = document.getElementById("github-branch-input");
  const branch = (input && input.value.trim()) || `feat/${currentProjectId}`;
  const checkboxes = Array.from(document.querySelectorAll(".ws-file-chk:checked"));
  const paths = checkboxes.map((c) => c.dataset.path).filter(Boolean);
  if (!paths.length) {
    showToast("No files selected.");
    return;
  }
  if (
    !window.confirm(
      `Delete ${paths.length} selected files from ${branch}?\n\n` +
        paths.slice(0, 5).join("\n") +
        (paths.length > 5 ? `\n...and ${paths.length - 5} more` : "") +
        "\n\nThis will remove them from the workspace and branch.",
    )
  ) {
    return;
  }
  const btnDelSel = document.getElementById("btn-ws-delete-selected");
  if (btnDelSel) { btnDelSel.disabled = true; btnDelSel.textContent = "Deleting…"; }
  try {
    await api(`projects/${currentProjectId}/workspace/delete`, {
      method: "POST",
      body: JSON.stringify({ branch, paths }),
    });
    if (paths.includes(workspaceOpenPath)) {
      workspaceOpenPath = "";
      const editor = document.getElementById("ws-editor");
      const label = document.getElementById("ws-open-path");
      if (editor) editor.value = "";
      if (label) label.textContent = "Select a file";
      syncWsLineNumbers();
    }
    showToast(`Deleted ${paths.length} files from ${branch}`);
    log("workspace", `Deleted ${paths.length} files from branch ${branch}: ${paths.join(", ")}`);
    await loadWorkspaceFiles(branch);
  } catch (err) {
    showToast("Delete failed: " + err.message);
    log("error", `Multi-file delete failed: ${err.message}`);
  } finally {
    if (btnDelSel) {
      btnDelSel.disabled = false;
      btnDelSel.textContent = `🗑️ Delete (${document.querySelectorAll(".ws-file-chk:checked").length})`;
    }
  }
}

async function deleteWorkspaceFile(path) {
  const input = document.getElementById("github-branch-input");
  const branch = (input && input.value.trim()) || `feat/${currentProjectId}`;
  const target = path || workspaceOpenPath;
  if (!target) {
    const checked = Array.from(document.querySelectorAll(".ws-file-chk:checked"));
    if (checked.length > 0) {
      return deleteSelectedWorkspaceFiles();
    }
    showToast("Select a file first.");
    return;
  }
  if (
    !window.confirm(
      "Delete " + target + " from " + branch + "? This creates a new commit.",
    )
  ) {
    return;
  }
  try {
    await api(`projects/${currentProjectId}/workspace/delete`, {
      method: "POST",
      body: JSON.stringify({ branch, path: target }),
    });
    if (workspaceOpenPath === target) {
      workspaceOpenPath = "";
      const editor = document.getElementById("ws-editor");
      const label = document.getElementById("ws-open-path");
      if (editor) editor.value = "";
      if (label) label.textContent = "Select a file";
      syncWsLineNumbers();
    }
    showToast("Deleted " + target + " from " + branch);
    await loadWorkspaceFiles(branch);
  } catch (err) {
    showToast("Delete failed: " + err.message);
  }
}

async function pushBlocksAndOpenVsCode() {
  const btn = document.getElementById("btn-push-blocks-tab");
  const btnWs = document.getElementById("btn-ws-commit-push");
  if (btn) btn.disabled = true;
  if (btnWs) btnWs.disabled = true;
  const checked = Array.from(document.querySelectorAll(".ws-file-chk:checked"));
  const selectedPaths = checked.map((c) => c.dataset.path).filter(Boolean);
  const msg = selectedPaths.length > 0
    ? `Committing and pushing ${selectedPaths.length} selected files to branch...`
    : "Committing and pushing workspace files to branch...";
  showToast(msg);
  try {
    await previewToBranch(selectedPaths.length > 0 ? selectedPaths : undefined);
    showToast(selectedPaths.length > 0
      ? `✅ Committed & pushed ${selectedPaths.length} files to branch!`
      : "✅ Committed & pushed workspace blocks to branch!");
  } catch (err) {
    showToast("Commit & push failed: " + err.message);
  } finally {
    if (btn) btn.disabled = false;
    if (btnWs) btnWs.disabled = false;
  }
}

async function checkBranchStatus() {
  const input = document.getElementById("github-branch-input");
  const branch = input ? input.value.trim() : "";
  const resultEl = document.getElementById("github-status-result");
  if (!branch) {
    showToast("Enter a branch name first.");
    return;
  }
  if (resultEl) resultEl.innerHTML = "Checking CI runs and changed files...";
  try {
    const data = await api(`projects/${currentProjectId}/branch-status`, {
      method: "POST",
      body: JSON.stringify({ branch }),
    });
    if (data.error) {
      if (resultEl)
        resultEl.innerHTML = `<span style="color:var(--danger,#c00)">${data.error}</span>`;
      return;
    }
    let html = "";
    if (data.vscodeUrl) {
      html += `<p><a href="${data.vscodeUrl}" target="_blank">🔗 Open branch '${data.branch}' in vscode.dev</a></p>`;
      loadVsCodeFrame(data.vscodeUrl);
    }
    if (data.latestRun) {
      const r = data.latestRun;
      html +=
        `<p><b>Latest CI run:</b> ${r.name || "workflow"} — status: <b>${r.status}</b>, conclusion: <b style="color:${r.conclusion === "success" ? "var(--accent)" : "var(--warn)"}">${r.conclusion || "pending"}</b> ` +
        (r.htmlUrl
          ? `(<a href="${r.htmlUrl}" target="_blank">view run logs</a>)`
          : "") +
        `</p>`;
    } else {
      html +=
        "<p><i>No GitHub Actions workflow run found for this branch (CI may not be configured on the repo, or no run has triggered yet).</i></p>";
    }
    const files = data.changedFiles || [];
    html += `<p><b>Changed files vs '${data.baseBranch}':</b> ${files.length}</p>`;
    if (files.length) {
      html +=
        "<table><thead><tr><th>File</th><th>Status</th><th>+/-</th></tr></thead><tbody>";
      files.forEach((f) => {
        html += `<tr><td><code>${f.filename}</code></td><td><span style="font-weight:700;">${f.status}</span></td><td><span style="color:var(--accent);">+${f.additions}</span> / <span style="color:var(--danger);">${f.deletions}</span></td></tr>`;
      });
      html += "</tbody></table>";
    }
    if (resultEl) resultEl.innerHTML = html;
  } catch (err) {
    if (resultEl)
      resultEl.innerHTML = `<span style="color:var(--danger,#c00)">Check failed: ${err.message}</span>`;
  }
}

function updateNpmRunProgress(logEl, data) {
  if (!logEl) return;
  if (data.logs) {
    logEl.textContent = data.logs;
  } else {
    logEl.textContent = `status=${data.status} conclusion=${data.conclusion || "pending"}\n`
      + (data.htmlUrl ? data.htmlUrl + "\n" : "");
  }
}

async function pollNpmRun(runId, onProgress) {
  for (let i = 0; i < 40; i++) {
    await new Promise((r) => setTimeout(r, 2500));
    const data = await api(
      `projects/${currentProjectId}/npm/${encodeURIComponent(runId)}`,
    );
    if (data.error) {
      throw new Error(data.error);
    }
    onProgress(data);
    if (
      data.status === "completed" ||
      data.status === "failure" ||
      data.status === "cancelled"
    ) {
      return data;
    }
  }
  return null;
}

function handleNpmHealResult(started, logEl) {
  if (logEl) {
    logEl.textContent += "Heal loop " + (started.status || "started")
      + (started.ciHeal ? " (" + started.ciHeal + ")" : "") + ".\n";
  }
  showToast("Heal CI: " + (started.ciHeal || started.status || "started"));
  log("npm", "Heal CI " + (started.status || "started"));
}

async function runNpmScript(command) {
  const logEl = document.getElementById("npm-log-terminal");
  const lintBtn = document.getElementById("btn-npm-lint");
  const jsonBtn = document.getElementById("btn-npm-json");
  const healBtn = document.getElementById("btn-npm-heal");
  if (lintBtn) lintBtn.disabled = true;
  if (jsonBtn) jsonBtn.disabled = true;
  if (healBtn) healBtn.disabled = true;
  if (logEl)
    logEl.textContent = `${command === "heal" ? "$ Heal CI" : "$ npm run " + command}\nDispatching GitHub Actions on ${featureBranchName()}...\n`;
  log("npm", `Dispatching npm run ${command} on ${featureBranchName()}...`);
  try {
    const started = await api(`projects/${currentProjectId}/npm`, {
      method: "POST",
      body: JSON.stringify({ command }),
    });
    if (started.error) {
      if (logEl) logEl.textContent += "ERROR: " + started.error + "\n";
      showToast("npm dispatch failed: " + started.error);
      return;
    }
    const runId = started.runId;
    if (started.logs && logEl) {
      logEl.textContent = started.logs;
    }
    if (command === "heal") {
      handleNpmHealResult(started, logEl);
      return;
    }
    if (!runId) {
      if (logEl) {
        logEl.textContent +=
          "Dispatched. Run id not available yet — check GitHub Actions.\n";
        if (started.htmlUrl) logEl.textContent += started.htmlUrl + "\n";
      }
      return;
    }
    if (started.status === "completed" && started.logs && logEl) {
      logEl.textContent = started.logs;
      showToast(`npm run ${command}: ${started.conclusion || started.status}`);
      return;
    }
    const finished = await pollNpmRun(runId, (data) =>
      updateNpmRunProgress(logEl, data),
    );
    if (finished) {
      showToast(
        `npm run ${command}: ${finished.conclusion || finished.status}`,
      );
      log(
        "npm",
        `npm run ${command} finished: ${finished.conclusion || finished.status}`,
      );
    } else {
      showToast(
        `npm run ${command}: still running after 100s — check GitHub Actions.`,
      );
    }
  } catch (err) {
    if (logEl) logEl.textContent += "ERROR: " + err.message + "\n";
    showToast("npm run failed: " + err.message);
  } finally {
    if (lintBtn) lintBtn.disabled = false;
    if (jsonBtn) jsonBtn.disabled = false;
    if (healBtn) healBtn.disabled = false;
  }
}

async function createPullRequest() {
  if (!vscodeReviewConfirmed) {
    showToast("Confirm you have reviewed the branch in VS Code first.");
    const chk = document.getElementById("chk-vscode-reviewed");
    if (chk) chk.focus();
    return;
  }
  const confirmed = confirm(
    `Open a Pull Request from ${featureBranchName()} for project ${currentProjectId}? Generated files will not be re-committed.`,
  );
  if (!confirmed) return;
  await runPushToGit();
}

async function loadInventorySection() {
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
}

async function loadGeneratedFilesSection() {
  const files = await api(`projects/${currentProjectId}/files`);
  if (files) {
    generatedFiles = files;
    processBlockFiles(files);
    try {
      const events = await api(`projects/${currentProjectId}/events`);
      applyReconcileBadges(events);
    } catch (e) {
      log("error", `Could not load reconcile events: ${e.message}`);
    }
    renderBlockList();
    if (activePagePath) {
      selectPageRow(activePagePath);
    }
  }
}

async function refreshDashboard() {
  const sections = [
    loadInventorySection,
    loadGeneratedFilesSection,
    loadPlanSection,
    loadRedirectsSection,
    loadDependenciesSection,
    loadRolloutSection,
    loadRepairsSection,
    loadBenchmarksSection,
    loadEventsSection,
  ];
  for (const loader of sections) {
    try {
      await loader();
    } catch (err) {
      log("error", `Dashboard refresh failed: ${err.message}`);
    }
  }
}

async function loadPlanSection() {

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
      renderEstimateTrail(plan.derivationTrail);
    }
  }
}

async function loadRedirectsSection() {
  const redirects = await api(`projects/${currentProjectId}/redirects`);
  renderRedirectsTable(redirects);
}

async function loadDependenciesSection() {
  const deps = await api(`projects/${currentProjectId}/dependencies`);
  renderDependenciesTable(deps);
}

async function loadRolloutSection() {
  const rollout = await api(`projects/${currentProjectId}/rollout-stages`);
  renderRolloutTable(rollout);
}

async function loadRepairsSection() {
  const repairs = await api(`projects/${currentProjectId}/repairs`);
  renderRepairsTable(repairs);
}

async function loadBenchmarksSection() {
  const benchmarks = await api(`projects/${currentProjectId}/benchmarks`);
  renderBenchmarksTable(benchmarks);
}

async function loadEventsSection() {
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
}

function processBlockFiles(files) {
  blockFilesMap = {};
  if (!files || files.length === 0) return blockFilesMap;

  files.forEach((f) => {
    const path = f.path || "";
    if (path.startsWith("blocks/")) {
      const parts = path.split("/");
      if (parts.length >= 3) {
        const bName = parts[1];
        const fileName = parts[parts.length - 1];
        if (!blockFilesMap[bName]) {
          blockFilesMap[bName] = {
            name: bName,
            files: {},
            sourcePath: null,
            reconcile: "Created",
          };
        }
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
  return blockFilesMap;
}

function applyReconcileBadges(events) {
  if (!events || !Array.isArray(events)) return;
  const msg = events
    .map((e) => (e && e.message) || "")
    .reverse()
    .find((m) => m.indexOf("Block reconcile:") >= 0);
  if (!msg) return;
  Object.keys(blockFilesMap).forEach((name) => {
    const re = new RegExp(
      "\\b" +
        name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") +
        "=(CREATE|LEAVE|ENHANCE)\\b",
      "i",
    );
    const m = msg.match(re);
    if (m) {
      const action = m[1].toUpperCase();
            blockFilesMap[name].reconcile =
              action === "LEAVE" ? "Left" : action === "ENHANCE" ? "Enhanced" : "Created";
          }
        });
        return blockFilesMap;
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
      return `<div class="block-item ${isActive ? "active" : ""}" data-name="${escapeHtml(name)}">
      <div class="block-item-title">
        <span>🧱</span>
        <span>${escapeHtml(name)}</span>
      </div>
      <span class="block-item-badge">${escapeHtml(b.reconcile || "Created")} · ${fileCount} files</span>
    </div>`;
    })
    .join("");

  if (!container.dataset.bound) {
    container.dataset.bound = "1";
    container.addEventListener("click", (ev) => {
      const item = ev.target.closest(".block-item");
      if (item && item.dataset.name) {
        selectBlock(item.dataset.name);
      }
    });
  }

  renderActiveBlockDetail();
}

function selectBlock(name) {
  activeBlockName = name;
  renderBlockList();
}

function switchBlockFileTab(tabName) {
  activeFileTab = tabName;
  ["demo", "da", "ue", "json", "js", "css", "readme"].forEach((t) => {
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
  const daContainer = document.getElementById("block-view-da");
  const ueContainer = document.getElementById("block-view-ue");

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

  if (daContainer) daContainer.style.display = "none";
  if (ueContainer) ueContainer.style.display = "none";

  if (activeFileTab === "da") {
    if (codeContainer) codeContainer.style.display = "none";
    if (demoContainer) demoContainer.style.display = "none";
    if (daContainer) daContainer.style.display = "block";
    const daEl = document.getElementById("block-da-rendered");
    if (daEl) daEl.innerHTML = renderBlockDaMarkup(b);
    return;
  }
  if (activeFileTab === "ue") {
    if (codeContainer) codeContainer.style.display = "none";
    if (demoContainer) demoContainer.style.display = "none";
    if (ueContainer) ueContainer.style.display = "block";
    const ueEl = document.getElementById("block-ue-rendered");
    if (ueEl) ueEl.innerHTML = renderBlockUeGuide(b);
    return;
  }

  if (activeFileTab === "demo") {
    if (codeContainer) codeContainer.style.display = "none";
    if (demoContainer) demoContainer.style.display = "block";

    const htmlContent = b.files && b.files.demo ? b.files.demo.content : "";

    if (demoRendered) {
      if (htmlContent) {
        // Render the actual compiled HTML block inside a sandboxed iframe to prevent styles leaking
        const cleanHtml = htmlContent.replace(/"/g, "&quot;");
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
let activePageTab = "html";

function renderPagesTable(pages) {
  const tbody = document.querySelector("#table-pages tbody");
  if (!tbody) return;
  tbody.innerHTML =
    pages && pages.length > 0
      ? pages
          .map(
            (p) =>
              `<tr data-path="${p.path}" onclick="AemEdsDashboard.selectPageRow('${p.path}')"><td style="word-break:break-all; padding: 10px;"><code>${p.path}</code></td><td>${p.title || "-"}</td></tr>`,
          )
          .join("")
      : '<tr><td colspan="2">No pages discovered yet.</td></tr>';

  if (pages && pages.length > 0 && !activePagePath) {
    selectPageRow(pages[0].path);
  }
}

function selectPageRow(path) {
  activePagePath = path;
  document
    .querySelectorAll("#table-pages tbody tr")
    .forEach((tr) => tr.classList.remove("active-row"));

  const tr = document.querySelector(
    `#table-pages tbody tr[data-path="${path}"]`,
  );
  if (tr) tr.classList.add("active-row");

  const fileObj = generatedFiles.find(
    (f) =>
      f.sourcePath === path &&
      f.path &&
      f.path.indexOf("docs/migrated-pages/") === 0 &&
      f.path.endsWith(".md"),
  );
  const pathLabel = document.getElementById("page-preview-path");
  if (pathLabel)
    pathLabel.textContent = fileObj ? fileObj.path : "No migrated file found";

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
        `<button class="chat-chip" style="font-size:0.75rem; padding:4px 10px;" title="AEM root path: ${b.sourcePath}" onclick="AemEdsDashboard.jumpToBlock('${b.name}')">🧱 ${b.name}</button>`,
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
  document
    .querySelectorAll(
      "#pagetab-preview, #pagetab-source, #pagetab-html, #pagetab-ue",
    )
    .forEach((b) => b.classList.remove("active"));
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
  const ueContainer = document.getElementById("page-view-ue");
  const ueRendered = document.getElementById("page-ue-rendered");

  const fileObj =
    generatedFiles.find(
      (f) =>
        f.sourcePath === activePagePath &&
        f.path &&
        f.path.indexOf("docs/migrated-pages/") === 0 &&
        f.path.endsWith(".md"),
    ) ||
    generatedFiles.find(
      (f) => f.fileType === "SECTION_MD" && f.sourcePath === activePagePath,
    );
  const markdown = fileObj ? fileObj.content : "";

  if (ueContainer) ueContainer.style.display = "none";

  if (activePageTab === "preview") {
    if (sourceContainer) sourceContainer.style.display = "none";
    if (htmlContainer) htmlContainer.style.display = "none";
    if (previewContainer) previewContainer.style.display = "block";
    if (previewRendered) {
      if (markdown) {
        const titleMatch = markdown.match(/^#\s+(.+)$/m);
        const title = (titleMatch && titleMatch[1]) || "";
        const daFile = generatedFiles.find(
          (f) => f.sourcePath === activePagePath && f.fileType === "DA_HTML",
        );
        const html =
          (daFile && daFile.content) ||
          daFromMarkdownClient(markdown, title, activePagePath);
        const paste = daPasteInner(html);
        window.__daPaste = paste;
        const daPath = daDocPathClient(activePagePath);
        previewRendered.innerHTML =
          `<p style="font-size:0.82rem;color:#334155;margin:0 0 8px;">DA document path (from AEM root): <code>${escapeAttr(daPath)}</code></p>` +
          `<p style="font-size:0.78rem;color:#64748b;margin:0 0 10px;">Paste these tables into Document Authoring. The first row of each table is the block name. Styled markdown tables will not render as blocks.</p>` +
          `<button type="button" class="btn btn-primary" onclick="AemEdsDashboard.copyDaPaste()">Copy for Document Authoring</button>` +
          `<div class="da-editor" style="background:#fff;color:#202124;padding:16px;margin-top:12px;border:1px solid #dadce0;">${daPasteInner(html)}</div>` +
          `<textarea id="da-paste-src" style="width:100%;min-height:180px;margin-top:10px;font-family:ui-monospace,Menlo,monospace;font-size:0.75rem;background:#0f172a;color:#e2e8f0;padding:12px;border-radius:6px;">${escapeAttr(paste)}</textarea>`;
      } else {
        previewRendered.innerHTML = `
          <div style="text-align:center; padding:40px 20px; color:#64748b;">
            No migrated markdown content available for this page. Click <b>Migrate Site</b> to generate.
          </div>
        `;
      }
    }
  } else if (activePageTab === "ue") {
    if (sourceContainer) sourceContainer.style.display = "none";
    if (htmlContainer) htmlContainer.style.display = "none";
    if (previewContainer) previewContainer.style.display = "none";
    if (ueContainer) ueContainer.style.display = "block";
    if (ueRendered) ueRendered.innerHTML = renderPageUeGuide(markdown);
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
      sourceContent.innerText =
        markdown || "// No migrated markdown content available for this page.";
    }
  }
}

function inferUeFieldType(name) {
  const n = String(name || "").toLowerCase();
  if (n.includes("image") || n.includes("file") || n.includes("asset"))
    return "reference";
  if (n.includes("link") || n.includes("url") || n.includes("path"))
    return "aem-content";
  if (
    n.includes("text") ||
    n.includes("title") ||
    n.includes("desc") ||
    n.includes("copy")
  )
    return "richtext";
  return "text";
}

function parseMarkdownBlockTables(markdown) {
  const blocks = [];
  if (!markdown) return blocks;
  let current = null;
  let headers = [];
  markdown.split("\n").forEach((raw) => {
    const line = raw.trim();
    if (line.startsWith("### ")) {
      current = { name: line.substring(4).trim(), fields: [] };
      blocks.push(current);
      headers = [];
      return;
    }
    if (!current || !line.startsWith("|")) return;
    const cols = line
      .split("|")
      .map((c) => c.trim())
      .filter((c, i, a) => i > 0 && i < a.length - 1);
    if (cols.every((c) => c.startsWith("-"))) return;
    if (!headers.length) {
      headers = cols;
      return;
    }
    cols.forEach((val, i) =>
      current.fields.push({ name: headers[i] || `col${i + 1}`, value: val }),
    );
  });
  return blocks;
}

function daDocPathClient(aemPath) {
  if (!aemPath) return "/index";
  let p = aemPath;
  if (p.startsWith("/content/wknd")) p = p.substring("/content/wknd".length);
  else if (p.startsWith("/content/")) {
    const i = p.indexOf("/", "/content/".length);
    if (i > 0) p = p.substring(i);
  }
  return p || "/index";
}

function daPasteInner(html) {
  const m = String(html || "").match(/<main>([\s\S]*?)<\/main>/i);
  return m ? m[1].trim() : String(html || "").trim();
}

function copyDaPaste() {
  const t = window.__daPaste || "";
  navigator.clipboard.writeText(t).then(() => {
    showToast("Copied DA HTML for " + daDocPathClient(activePagePath));
  });
}

function daTableHtml(name, rows) {
  let width = 1;
  (rows || []).forEach((r) => {
    width = Math.max(width, r.length || 1);
  });
  const header = name === "Metadata" ? "Metadata" : name;
  let s = `<table>\n<tr><td colspan="${width}">${escapeAttr(header)}</td></tr>\n`;
  if (!rows || !rows.length) s += "<tr><td></td></tr>\n";
  (rows || []).forEach((r) => {
    s += "<tr>";
    for (let i = 0; i < width; i++) {
      const c = r[i] || "";
      const lower = String(c).toLowerCase();
      const cell =
        lower.startsWith("/content/dam/") ||
        /\.(png|jpe?g|gif|webp|svg)$/.test(lower)
          ? `<img src="${escapeAttr(c)}" alt="">`
          : escapeAttr(c);
      s += `<td>${cell}</td>`;
    }
    s += "</tr>\n";
  });
  return s + "</table>\n";
}

function daFromMarkdownClient(markdown, title, aemPath) {
  const blocks = parseMarkdownBlockTables(markdown);
  let inner = `<h1>${escapeAttr(title || "Page")}</h1>`;
  blocks.forEach((b) => {
    const rows = (b.fields || []).map((f) => [f.name, f.value]);
    inner += daTableHtml(b.name, rows);
  });
  inner += daTableHtml("Metadata", [
    ["title", title || ""],
    ["source-path", aemPath || ""],
  ]);
  return `<body>\n<header></header>\n<main>\n<div>\n${inner}\n</div>\n</main>\n<footer></footer>\n</body>`;
}

function buildDaDocument(markdown) {
  return daFromMarkdownClient(
    markdown,
    (markdown.match(/^#\s+(.+)$/m) || [])[1] || "",
    activePagePath,
  );
}

function renderPageUeGuide(markdown) {
  const blocks = parseMarkdownBlockTables(markdown);
  const mapped = getBlocksForPagePath(activePagePath);
  let html = `<div class="ue-guide" style="color:#0f172a;">
    <h3 style="margin:0 0 8px;">Author this page in Universal Editor</h3>
    <ol style="margin:0 0 16px 18px; line-height:1.6;">
      <li>Open the page in AEM Universal Editor (authoring strategy UNIVERSAL_EDITOR).</li>
      <li>Keep the JCR source <code>${escapeAttr(activePagePath || "")}</code>.</li>
      <li>Insert each block from the component palette. Fields map to <code>_<block>.json</code> / component-models.json.</li>
      <li>Migrated markdown is stored under <code>docs/migrated-pages/</code>. Do not edit fstab.yaml.</li>
    </ol>`;
  const names = new Set(blocks.map((b) => b.name));
  mapped.forEach((b) => names.add(b.name));
  if (!names.size) {
    html += `<p>Generate the site to produce UE field instructions for this page.</p></div>`;
    return html;
  }
  blocks.forEach((b) => {
    html += `<h3 style="font-size:1rem;">Block: ${escapeAttr(b.name)}</h3>
      <table style="width:100%; border-collapse:collapse; margin-bottom:12px; font-size:0.82rem;">
      <thead><tr><th style="text-align:left;border-bottom:1px solid #cbd5e1;padding:6px;">Field</th><th style="text-align:left;border-bottom:1px solid #cbd5e1;padding:6px;">UE component</th><th style="text-align:left;border-bottom:1px solid #cbd5e1;padding:6px;">Sample</th></tr></thead><tbody>`;
    b.fields.forEach((f) => {
      html += `<tr><td style="padding:6px;border-bottom:1px solid #e2e8f0;"><code>${escapeAttr(f.name)}</code></td><td style="padding:6px;border-bottom:1px solid #e2e8f0;">${escapeAttr(inferUeFieldType(f.name))}</td><td style="padding:6px;border-bottom:1px solid #e2e8f0;">${escapeAttr(f.value)}</td></tr>`;
    });
    html += `</tbody></table>`;
  });
  mapped.forEach((b) => {
    const json = b.files && b.files.json ? b.files.json.content : "";
    html += renderBlockUeGuide(b, json);
  });
  return html + `</div>`;
}

function renderBlockDaMarkup(b) {
  const fields = ueFieldsFromBlock(b);
  const rows = fields.length ? fields.map((f) => [f.name, ""]) : [];
  const table = daTableHtml(b.name, rows);
  window.__daPaste = table;
  return `<p style="font-size:0.82rem;color:#334155;">Paste this table into Document Authoring. First row is the block name (colspan).</p>
    <button type="button" class="btn btn-primary" onclick="AemEdsDashboard.copyDaPaste()">Copy for Document Authoring</button>
    <div class="da-editor" style="background:#fff;color:#202124;padding:16px;margin-top:12px;border:1px solid #dadce0;">${table}</div>
    <textarea style="width:100%;min-height:140px;margin-top:10px;font-family:ui-monospace,Menlo,monospace;font-size:0.75rem;background:#0f172a;color:#e2e8f0;padding:12px;">${escapeAttr(table)}</textarea>`;
}

function ueFieldsFromBlock(b) {
  const raw = b.files && b.files.json ? b.files.json.content : "";
  if (!raw) return [];
  try {
    const j = JSON.parse(raw);
    const models = Array.isArray(j.models)
      ? j.models
      : Array.isArray(j)
        ? j
        : j.models
          ? [j.models]
          : [];
    const fields = [];
    models.forEach((m) =>
      (m.fields || []).forEach((f) =>
        fields.push({
          name: f.name || f.id || "",
          component: f.component || inferUeFieldType(f.name),
        }),
      ),
    );
    return fields;
  } catch (e) {
    return [];
  }
}

function renderBlockUeGuide(b) {
  const fields = ueFieldsFromBlock(b);
  let html = `<div class="ue-guide" style="color:#0f172a;">
    <h3 style="margin:0 0 8px;">Author <code>${escapeAttr(b.name)}</code> in Universal Editor</h3>
    <ol style="margin:0 0 12px 18px; line-height:1.6;">
      <li>Use the generated model <code>blocks/${escapeAttr(b.name)}/_${escapeAttr(b.name)}.json</code>.</li>
      <li>In UE, insert the block from the component palette onto the section.</li>
      <li>Fill the property panel. Images use <code>reference</code>, links use <code>aem-content</code>, rich copy uses <code>richtext</code>.</li>
    </ol>`;
  if (fields.length) {
    html += `<table style="width:100%;border-collapse:collapse;font-size:0.82rem;"><thead><tr><th style="text-align:left;padding:6px;border-bottom:1px solid #cbd5e1;">Field</th><th style="text-align:left;padding:6px;border-bottom:1px solid #cbd5e1;">Component</th></tr></thead><tbody>`;
    fields.forEach((f) => {
      html += `<tr><td style="padding:6px;border-bottom:1px solid #e2e8f0;"><code>${escapeAttr(f.name)}</code></td><td style="padding:6px;border-bottom:1px solid #e2e8f0;">${escapeAttr(f.component || inferUeFieldType(f.name))}</td></tr>`;
    });
    html += `</tbody></table>`;
  } else {
    html += `<p>Generate blocks to create the UE model for this component.</p>`;
  }
  if (b.files && b.files.readme && b.files.readme.content) {
    html += `<h3 style="font-size:1rem;margin-top:16px;">README</h3><pre style="white-space:pre-wrap;font-size:0.78rem;background:#0f172a;color:#e2e8f0;padding:12px;border-radius:6px;">${escapeAttr(b.files.readme.content)}</pre>`;
  }
  return html + `</div>`;
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
  else
    body = demoHtml.replace(
      /<html[^>]*>|<\/html>|<head[\s\S]*?<\/head>|<!doctype[^>]*>/gi,
      "",
    );
  body = body.replace(/<script[\s\S]*?<\/script>/gi, "");
  return { styles, body };
}

function escapeAttr(text) {
  return (text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function buildPageHtmlDocument(pagePath, markdown) {
  const blocks = getBlocksForPagePath(pagePath);
  const pageTitle = (
    pagePath ? pagePath.split("/").filter(Boolean).pop() : "page"
  )
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
      (body ||
        `<div class="page-block-empty">No authored demo HTML generated yet for this block.</div>`) +
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
  if (!markdown)
    return '<div style="text-align:center;color:#64748b;">No content available.</div>';

  const lines = markdown.split("\n");
  let html = "";
  let inTable = false;
  let tableRows = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    if (line.startsWith("|")) {
      inTable = true;
      const cols = line
        .split("|")
        .map((c) => c.trim())
        .filter((c, idx, arr) => idx > 0 && idx < arr.length - 1);
      tableRows.push(cols);
      continue;
    } else {
      if (inTable) {
        html += renderDATable(tableRows);
        inTable = false;
        tableRows = [];
      }
    }

    if (line.startsWith("# ")) {
      html += `<h1 style="font-size:1.8rem; font-weight:800; border-bottom:2px solid #e2e8f0; padding-bottom:8px; margin-top:20px; margin-bottom:12px; color:#0f172a;">${line.substring(2)}</h1>`;
    } else if (line.startsWith("## ")) {
      html += `<h2 style="font-size:1.4rem; font-weight:700; margin-top:16px; margin-bottom:10px; color:#1e293b;">${line.substring(3)}</h2>`;
    } else if (line.startsWith("### ")) {
      html += `<h3 style="font-size:1.15rem; font-weight:700; margin-top:14px; margin-bottom:8px; color:#334155;">${line.substring(4)}</h3>`;
    } else if (line === "---" || line === "***") {
      html +=
        '<hr style="border:0; border-top:2px dashed #cbd5e1; margin:20px 0;">';
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
  if (rows.length === 0) return "";
  if (
    rows.length > 1 &&
    rows[1].every((col) => col.startsWith("-") || col.endsWith("-"))
  ) {
    rows.splice(1, 1);
  }

  let html =
    '<table style="width:100%; border:2px solid #2563eb; border-collapse:collapse; margin:14px 0; background:#f8fafc; font-family:var(--font-mono); font-size:0.8rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">';
  rows.forEach((row, rIdx) => {
    const isHeader = rIdx === 0;
    html += `<tr style="${isHeader ? "background:#dbeafe; color:#1e40af; font-weight:bold; border-bottom:2px solid #2563eb;" : "border-bottom:1px solid #cbd5e1;"}">`;
    row.forEach((col) => {
      html += `<td style="padding:8px 10px; border-right:1px solid #cbd5e1;">${col}</td>`;
    });
    html += "</tr>";
  });
  html += "</table>";
  return html;
}

function renderComponentsTable(components) {
  const list = document.getElementById("table-components");
  if (!list) return;
  list.innerHTML =
    components && components.length > 0
      ? components
          .map(
            (c) =>
              `<li class="data-list-item">` +
              `<div><div class="data-list-title">${escapeAttr(c.title || c.proposedEdsBlock || "Untitled")}</div>` +
              `<div class="data-list-meta">${escapeAttr(c.resourceType || "-")}${c.group ? " · " + escapeAttr(c.group) : ""}</div></div>` +
              `<div><div class="data-list-block">${escapeAttr(c.proposedEdsBlock || "-")}</div>` +
              `<div class="data-list-badge">${escapeAttr(c.capabilityClassification || "SUPPORTED")}</div></div>` +
              `</li>`,
          )
          .join("")
      : '<li class="data-list-empty">No components analyzed yet.</li>';
}

function renderEstimateTrail(steps) {
  const list = document.getElementById("estimate-trail");
  if (!list) return;
  if (!steps || !steps.length) {
    list.innerHTML =
      '<li class="data-list-empty">Run a Dry Run to compute the estimate trail.</li>';
    return;
  }
  list.innerHTML = steps
    .map((step) => `<li>${escapeAttr(String(step))}</li>`)
    .join("");
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
  {
    re: /run (a )?dry[ -]?run/i,
    action: () => {
      showTab("overview");
      runDryRun();
      return (
        "🔍 Starting dry run for project `" +
        currentProjectId +
        "` — watch the pipeline stepper."
      );
    },
  },
  {
    re: /(generate blocks|run migration|generate (the )?blocks)/i,
    action: () => {
      showTab("components");
      runMigration();
      return "⚡ Generating blocks for project `" + currentProjectId + "`.";
    },
  },
  {
    re: /(commit|push).*(git|github)|create pr|publish/i,
    action: () => {
      showTab("overview");
      runPushToGit();
      return "🚀 Creating a Pull Request.";
    },
  },
  {
    re: /show (me )?(the )?(live )?(events|overview( activity)? stream)/i,
    action: () => {
      showTab("overview");
      refreshDashboard();
      return "📡 Opened Overview — activity stream is on this tab.";
    },
  },
  {
    re: /show (me )?(the )?(generated )?blocks/i,
    action: () => {
      showTab("components");
      refreshDashboard();
      return "📦 Opened the Generated Blocks tab and refreshed the data.";
    },
  },
  {
    re: /show (me )?(the )?(pages|scope|discovered)/i,
    action: () => {
      showTab("pages");
      refreshDashboard();
      return "📄 Opened the Pages & Scope tab.";
    },
  },
  {
    re: /show (me )?(the )?estimate|cost/i,
    action: () => {
      showTab("estimate");
      refreshDashboard();
      return "💰 Opened the Estimate & Cost tab.";
    },
  },
  {
    re: /show (me )?(the )?github|checks/i,
    action: () => {
      showTab("github");
      return "🔍 Opened the GitHub Checks tab.";
    },
  },
  {
    re: /^refresh( dashboard)?$/i,
    action: () => {
      refreshDashboard();
      return "🔄 Dashboard refreshed.";
    },
  },
];

function tryChatCommand(msg) {
  for (const cmd of CHAT_COMMANDS) {
    if (cmd.re.test(msg)) {
      let reply;
      try {
        reply = cmd.action();
      } catch (e) {
        reply = "⚠️ Could not run that action: " + e.message;
      }
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
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>");

  // Bold: **text**
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");

  // Bullet lists: - item or * item
  html = html.replace(/^\s*[-*]\s+(.+)$/gm, "<li>$1</li>");

  // Wrap list items in ul
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, "<ul>$1</ul>");

  // Clean up duplicate consecutive ul elements
  html = html.replace(/<\/ul>\s*<ul>/g, "");

  // Paragraphs / line breaks (only when not inside ul/pre/code tags)
  html = html
    .split("\n")
    .map((line) => {
      const trimmed = line.trim();
      if (
        trimmed.startsWith("<pre") ||
        trimmed.startsWith("<ul") ||
        trimmed.startsWith("<li") ||
        trimmed.startsWith("</ul") ||
        trimmed.startsWith("</li") ||
        trimmed.startsWith("<code>") ||
        trimmed.startsWith("</pre>") ||
        trimmed.startsWith("</ul>")
      ) {
        return line;
      }
      return line ? `<p>${line}</p>` : "";
    })
    .join("\n");

  return html;
}

function appendChatMessage(role, text) {
  const wrap = document.getElementById("chat-messages");
  if (!wrap) return;
  const div = document.createElement("div");
  div.className =
    "chat-msg " + (role === "user" ? "chat-msg-user" : "chat-msg-agent");
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
    const events = await api(
      "/projects/" + encodeURIComponent(currentProjectId) + "/events",
    );
    if (Array.isArray(events)) {
      const chatEvents = events.filter(
        (e) => e.agent === "chat-user" || e.agent === "chat-agent",
      );
      if (chatEvents.length > 0) {
        document.getElementById("chat-messages").innerHTML = "";
        chatEvents.forEach((e) => {
          appendChatMessage(
            e.agent === "chat-user" ? "user" : "agent",
            e.message || "",
          );
          chatHistory.push({
            role: e.agent === "chat-user" ? "user" : "agent",
            text: e.message || "",
          });
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
    const res = await api(
      "/projects/" + encodeURIComponent(currentProjectId) + "/chat",
      {
        method: "POST",
        body: JSON.stringify({ message: msg, history: chatHistory.slice(-10) }),
      },
    );
    typing.remove();
    appendChatMessage("agent", res.reply || "(no response)");
    chatHistory.push({ role: "agent", text: res.reply || "" });
  } catch (e) {
    typing.remove();
    const detail = e && e.message ? e.message : String(e);
    appendChatMessage(
      "agent",
      "⚠️ Error talking to the agent: " +
        (detail || "unknown error — check browser console & AEM logs"),
    );
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
    } else {
      onProviderChange();
    }
  } catch (e) {
    console.log("Dashboard init:", e);
  }
});

function toggleCardCollapse(bodyId, arrowId) {
  const body = document.getElementById(bodyId);
  const arrow = document.getElementById(arrowId);
  if (!body) return;
  const isHidden = body.style.display === "none";
  body.style.display = isHidden ? "block" : "none";
  if (arrow) {
    arrow.textContent = isHidden ? "▼" : "▶";
  }
}

/** Single namespace exposed to the markup and JS-generated inline handlers. */
window.AemEdsDashboard = {
    toggleCardCollapse,
    // navigation & pipeline
    showTab,
    runDryRun,
    runMigration,
    runPushToGit,
    createPullRequest,
    previewToBranch,
    refreshDashboard,
    deleteCurrentProject,
    // project config form
    onProjectSelectChange,
    onQuickScopeChange,
    onProviderChange,
    loadWkndPreset,
    clearForm,
    saveProjectConfig,
    // blocks inspector
    switchBlockFileTab,
    switchPageFileTab,
    copyActiveCode,
    copyDaPaste,
    selectBlock,
    selectPageRow,
    jumpToBlock,
    // github / workspace
    pushBlocksAndOpenVsCode,
    checkBranchStatus,
    reloadVsCodeFrame,
    loadVsCodeFrame,
    setWsViewMode,
    createNewWorkspaceFilePrompt,
    saveWorkspaceFile,
    deleteWorkspaceFile,
    deleteSelectedWorkspaceFiles,
    toggleSelectAllWorkspaceFiles,
    onWorkspaceFileCheckboxChange,
    onVsCodeReviewToggle,
    runNpmScript,
    aemUpControl,
    runAiCompare,
    runAiCompareFromDevServer,
    // editor gutter
    syncWsLineNumbers,
    syncWsLineScroll,
    // chat
    sendChat,
    quickChat,
    // helpers (exported for unit tests)
    processBlockFiles,
    applyReconcileBadges,
    escapeHtml,
  };
})();
