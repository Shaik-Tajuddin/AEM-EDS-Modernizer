let currentProjectId = 'wknd-site';
let projectsList = [];

const api = (path, opts) => {
  const base = (document.querySelector('base')||{}).href || '/bin/aem-eds-modernizer/api/';
  return fetch(base + path, opts).then(r => r.json());
};

function showToast(msg) {
  const t = document.getElementById('toast');
  if (!t) return;
  t.innerText = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 3500);
}

function showTab(tabId) {
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
  const btn = Array.from(document.querySelectorAll('.nav-tab')).find(b => b.getAttribute('onclick') && b.getAttribute('onclick').includes(tabId));
  if (btn) btn.classList.add('active');
  const target = document.getElementById('tab-' + tabId);
  if (target) target.style.display = 'block';
}

function setPipelineStep(stepId, state) {
  const el = document.getElementById('step-' + stepId);
  if (el) {
    if (state === 'done') { el.className = 'step-item done'; }
    else if (state === 'active') { el.className = 'step-item active'; }
    else { el.className = 'step-item'; }
  }
}

function log(agent, msg) {
  const term = document.getElementById('terminal');
  const eventsLog = document.getElementById('events-log');
  const time = new Date().toLocaleTimeString();
  const line = `<div class="log-line"><span class="log-time">[${time}]</span><span class="log-agent">[${agent}]</span><span>${msg}</span></div>`;
  if (term) { term.innerHTML += line; term.scrollTop = term.scrollHeight; }
  if (eventsLog) { eventsLog.innerHTML += line; eventsLog.scrollTop = eventsLog.scrollHeight; }
}

function onProviderChange() {
  const provider = document.getElementById('cfg-aiProvider').value;
  const modelInput = document.getElementById('cfg-aiModel');
  if (!modelInput) return;
  if (provider === 'anthropic') modelInput.value = 'claude-3-5-sonnet-20241022';
  else if (provider === 'openai') modelInput.value = 'gpt-4o';
  else if (provider === 'gemini') modelInput.value = 'gemini-1.5-pro';
  else if (provider === 'ollama') modelInput.value = 'llama3:8b';
}

function loadWkndPreset() {
  document.getElementById('cfg-id').value = 'wknd-site';
  document.getElementById('cfg-name').value = 'WKND Site Modernization';
  document.getElementById('cfg-authorUrl').value = 'http://localhost:4502';
  document.getElementById('cfg-publishUrl').value = 'http://localhost:4503';
  document.getElementById('cfg-contentRoot').value = '/content/wknd';
  document.getElementById('cfg-pageScope').value = '/content/wknd/*';
  document.getElementById('cfg-repoUrl').value = 'https://github.com/my-org/wknd-eds';
  document.getElementById('cfg-branch').value = 'main';
  document.getElementById('cfg-markerProp').value = 'edsModernize';
  document.getElementById('cfg-markerVal').value = 'true';
  document.getElementById('cfg-authoringStrategy').value = 'UNIVERSAL_EDITOR';
  document.getElementById('cfg-aiProvider').value = 'anthropic';
  document.getElementById('cfg-aiModel').value = 'claude-3-5-sonnet-20241022';
  document.getElementById('cfg-maxBudget').value = '100.00';
  document.getElementById('cfg-maxRepair').value = '5';
  showToast('Loaded WKND Site Configuration Preset');
}

function clearForm() {
  document.getElementById('cfg-id').value = 'project-' + Math.random().toString(36).substring(2,7);
  document.getElementById('cfg-name').value = '';
  document.getElementById('cfg-authorUrl').value = 'http://localhost:4502';
  document.getElementById('cfg-publishUrl').value = '';
  document.getElementById('cfg-contentRoot').value = '/content/';
  document.getElementById('cfg-pageScope').value = '';
  document.getElementById('cfg-repoUrl').value = '';
  document.getElementById('cfg-figmaUrl').value = '';
}

async function saveProjectConfig() {
  const payload = {
    id: document.getElementById('cfg-id').value.trim() || 'project-1',
    name: document.getElementById('cfg-name').value.trim() || 'Untitled Project',
    aemAuthorUrl: document.getElementById('cfg-authorUrl').value.trim(),
    aemPublishUrl: document.getElementById('cfg-publishUrl').value.trim(),
    contentRoot: document.getElementById('cfg-contentRoot').value.trim(),
    pageScope: document.getElementById('cfg-pageScope').value.trim(),
    edsGitRepoUrl: document.getElementById('cfg-repoUrl').value.trim(),
    edsBranch: document.getElementById('cfg-branch').value.trim(),
    figmaUrl: document.getElementById('cfg-figmaUrl').value.trim(),
    markerProperty: document.getElementById('cfg-markerProp').value.trim(),
    markerValue: document.getElementById('cfg-markerVal').value.trim(),
    authoringStrategy: document.getElementById('cfg-authoringStrategy').value,
    aiProvider: document.getElementById('cfg-aiProvider').value,
    aiModel: document.getElementById('cfg-aiModel').value.trim(),
    maxBudgetUsd: parseFloat(document.getElementById('cfg-maxBudget').value) || 100.0,
    maxRepairAttempts: parseInt(document.getElementById('cfg-maxRepair').value, 10) || 5
  };

  log('connection', `Saving project '${payload.name}' (${payload.id}) & verifying AEM endpoint...`);
  try {
    const saved = await api('projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    currentProjectId = saved.id;
    log('connection', `Project saved successfully: ContentRoot=${payload.contentRoot}, EDS Repo=${payload.edsGitRepoUrl}`);
    showToast(`Project '${saved.name}' Saved & Connected!`);
    document.getElementById('btn-dryrun').disabled = false;
    setPipelineStep('connect', 'done');
    setPipelineStep('dryrun', 'active');
    await loadProjectsList();
    showTab('overview');
  } catch (err) {
    log('error', `Failed to save project: ${err.message}`);
    showToast('Error saving project');
  }
}

async function loadProjectsList() {
  try {
    projectsList = await api('projects');
    const select = document.getElementById('project-select');
    if (select && projectsList && projectsList.length > 0) {
      select.innerHTML = projectsList.map(p => `<option value="${p.id}" ${p.id === currentProjectId ? 'selected' : ''}>${p.name || p.id}</option>`).join('')
        + '<option value="new">+ Create New Project...</option>';
    }
  } catch (e) { console.log(e); }
}

async function onProjectSelectChange() {
  const val = document.getElementById('project-select').value;
  if (val === 'new') {
    clearForm();
    showTab('setup');
  } else {
    currentProjectId = val;
    await populateFormFromProject(val);
    await refreshDashboard();
  }
}

async function populateFormFromProject(id) {
  const p = projectsList.find(x => x.id === id) || await api(`projects/${id}`);
  if (p) {
    document.getElementById('cfg-id').value = p.id || '';
    document.getElementById('cfg-name').value = p.name || '';
    document.getElementById('cfg-authorUrl').value = p.aemAuthorUrl || 'http://localhost:4502';
    document.getElementById('cfg-publishUrl').value = p.aemPublishUrl || '';
    document.getElementById('cfg-contentRoot').value = p.contentRoot || '/content/wknd';
    document.getElementById('cfg-pageScope').value = p.pageScope || '';
    document.getElementById('cfg-repoUrl').value = p.edsGitRepoUrl || '';
    document.getElementById('cfg-branch').value = p.edsBranch || 'main';
    document.getElementById('cfg-figmaUrl').value = p.figmaUrl || '';
    document.getElementById('cfg-markerProp').value = p.markerProperty || 'edsModernize';
    document.getElementById('cfg-markerVal').value = p.markerValue || 'true';
    document.getElementById('cfg-authoringStrategy').value = p.authoringStrategy || 'UNIVERSAL_EDITOR';
    document.getElementById('cfg-aiProvider').value = p.aiProvider || 'anthropic';
    document.getElementById('cfg-aiModel').value = p.aiModel || 'claude-3-5-sonnet-20241022';
    document.getElementById('cfg-maxBudget').value = p.maxBudgetUsd || 100.0;
    document.getElementById('cfg-maxRepair').value = p.maxRepairAttempts || 5;
  }
}

async function runDryRun() {
  document.getElementById('btn-dryrun').disabled = true;
  setPipelineStep('dryrun', 'active');
  log('orchestrator', `Starting Mandatory Dry Run for project '${currentProjectId}'...`);
  try {
    const job = await api(`projects/${currentProjectId}/dryrun`, { method: 'POST' });
    log('orchestrator', `Dry Run execution completed with state: ${job.state}`);
    setPipelineStep('dryrun', 'done');
    setPipelineStep('analyze', 'done');
    setPipelineStep('contract', 'active');
    await refreshDashboard();
    document.getElementById('btn-migrate').disabled = false;
    showToast('Dry Run Completed! Review components, estimate & contract.');
  } catch (err) {
    log('error', `Dry run failed: ${err.message}`);
  } finally {
    document.getElementById('btn-dryrun').disabled = false;
  }
}

async function runMigration() {
  document.getElementById('btn-migrate').disabled = true;
  setPipelineStep('contract', 'done');
  setPipelineStep('migrate', 'active');
  log('orchestrator', `Executing Approved Migration pipeline for project '${currentProjectId}'...`);
  try {
    const job = await api(`projects/${currentProjectId}/migrate`, { method: 'POST' });
    log('orchestrator', `Migration finished with state: ${job.state}`);
    setPipelineStep('migrate', 'done');
    setPipelineStep('verify', 'done');
    await refreshDashboard();
    showToast('Migration Pipeline Completed Successfully!');
  } catch (err) {
    log('error', `Migration failed: ${err.message}`);
  }
}

async function refreshDashboard() {
  try {
    const inv = await api(`projects/${currentProjectId}/inventory`);
    if (inv && inv.pages) {
      document.getElementById('stat-pages').innerText = inv.pages.length;
      document.getElementById('stat-eligible').innerText = (inv.eligiblePages || inv.pages.length) + ' eligible';
      document.getElementById('stat-components').innerText = inv.components ? inv.components.length : 0;
      renderPagesTable(inv.pages);
      renderComponentsTable(inv.components);
    }
  } catch (e) {}

  try {
    const plan = await api(`projects/${currentProjectId}/plan`);
    if (plan) {
      document.getElementById('stat-cost').innerText = '$' + (plan.costExpected || 0).toFixed(2);
      document.getElementById('stat-requests').innerText = (plan.aiRequestsExpected || 0) + ' AI calls estimated';
      document.getElementById('stat-time').innerText = (plan.timeExpectedSec || 0) + 's';
      document.getElementById('stat-range').innerText = `Lo: ${(plan.timeOptimisticSec||0)}s | Hi: ${(plan.timePessimisticSec||0)}s`;
      if (plan.derivationTrail) {
        document.getElementById('estimate-trail').innerText = plan.derivationTrail.join('\n');
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
}

function renderPagesTable(pages) {
  const tbody = document.querySelector('#table-pages tbody');
  if (!tbody) return;
  tbody.innerHTML = (pages && pages.length > 0)
    ? pages.map(p => `<tr><td><code>${p.path}</code></td><td>${p.title || '-'}</td><td>${p.template || '-'}</td><td><span style="color:${p.eligible !== false ? 'var(--accent)' : 'var(--danger)'}; font-weight:700;">${p.eligible !== false ? '● ELIGIBLE' : '○ EXCLUDED'}</span></td></tr>`).join('')
    : '<tr><td colspan="4">No pages discovered yet.</td></tr>';
}

function renderComponentsTable(components) {
  const tbody = document.querySelector('#table-components tbody');
  if (!tbody) return;
  tbody.innerHTML = (components && components.length > 0)
    ? components.map(c => `<tr><td><code>${c.resourceType}</code></td><td>${c.title || '-'}</td><td>${c.group || '-'}</td><td><b style="color:var(--primary);">${c.proposedEdsBlock || '-'}</b></td><td><span style="background:rgba(56,189,248,0.1); color:var(--primary); padding:3px 8px; border-radius:4px; font-size:0.75rem; font-weight:700;">${c.capabilityClassification || 'SUPPORTED'}</span></td></tr>`).join('')
    : '<tr><td colspan="5">No components analyzed yet.</td></tr>';
}

function renderRedirectsTable(list) {
  const tbody = document.querySelector('#table-redirects tbody');
  if (!tbody) return;
  tbody.innerHTML = (list && list.length > 0)
    ? list.map(r => `<tr><td><code>${r.sourceUrl}</code></td><td><code>${r.targetUrl}</code></td><td><span style="color:var(--accent); font-weight:700;">${r.statusCode || 301}</span></td><td>${r.conflict ? '<span style="color:var(--warn);">⚠️ Conflict</span>' : '<span style="color:var(--accent);">OK</span>'}</td></tr>`).join('')
    : '<tr><td colspan="4">No redirects mapped.</td></tr>';
}

function renderDependenciesTable(list) {
  const tbody = document.querySelector('#table-dependencies tbody');
  if (!tbody) return;
  tbody.innerHTML = (list && list.length > 0)
    ? list.map(d => `<tr><td><code>${d.source}</code></td><td><code>${d.target}</code></td><td><span style="color:var(--primary);">${d.edgeType}</span></td><td>${d.impactLevel || 'LOW'}</td></tr>`).join('')
    : '<tr><td colspan="4">No dependencies computed.</td></tr>';
}

function renderRolloutTable(list) {
  const tbody = document.querySelector('#table-rollout tbody');
  if (!tbody) return;
  tbody.innerHTML = (list && list.length > 0)
    ? list.map(s => `<tr><td>#${s.stageIndex}</td><td><b>${s.stageName}</b></td><td><span style="color:var(--accent); font-weight:700;">${s.targetTrafficPercent}%</span></td><td>${s.status}</td></tr>`).join('')
    : '<tr><td colspan="4">No rollout stages initialized.</td></tr>';
}

function renderRepairsTable(list) {
  const tbody = document.querySelector('#table-repairs tbody');
  if (!tbody) return;
  tbody.innerHTML = (list && list.length > 0)
    ? list.map(r => `<tr><td><code>${r.targetPath}</code></td><td>#${r.attemptNumber}</td><td>${r.issueCategory || 'STYLE'}</td><td>${r.successful ? '<span style="color:var(--accent);">✅ Fixed</span>' : '<span style="color:var(--danger);">❌ Failed</span>'}</td></tr>`).join('')
    : '<tr><td colspan="4">No repair attempts recorded.</td></tr>';
}

function renderBenchmarksTable(list) {
  const tbody = document.querySelector('#table-benchmarks tbody');
  if (!tbody) return;
  tbody.innerHTML = (list && list.length > 0)
    ? list.map(b => `<tr><td><b>${b.agent}</b></td><td>${b.operation}</td><td>${b.durationMs}ms</td><td>${(b.costMicros || 0).toFixed(1)}</td></tr>`).join('')
    : '<tr><td colspan="4">No benchmark samples recorded.</td></tr>';
}

window.addEventListener('load', async () => {
  try {
    await loadProjectsList();
    if (projectsList && projectsList.length > 0) {
      currentProjectId = projectsList[0].id;
      await populateFormFromProject(currentProjectId);
      await refreshDashboard();
    }
  } catch (e) {
    console.log('Dashboard init:', e);
  }
});
