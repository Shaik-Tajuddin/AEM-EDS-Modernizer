"use strict";
/**
 * Unit tests for the pure-logic helpers in dashboard.js
 * (processBlockFiles / applyReconcileBadges / escapeHtml).
 *
 * Run with: node --test ui.apps/src/main/content/jcr_root/apps/aem-eds-modernizer/clientlibs/clientlib-dashboard/js/
 *
 * dashboard.js is a browser clientlib script, so it is evaluated in a vm
 * sandbox with minimal DOM/window stubs and the helpers are pulled from the
 * AemEdsDashboard namespace the IIFE exposes on window.
 */
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { join } = require("node:path");
const vm = require("node:vm");

const source = readFileSync(join(__dirname, "dashboard.js"), "utf8");

function loadDashboard() {
  const sandbox = {
    console: { log: () => {}, warn: () => {}, error: () => {} },
    setTimeout: () => 0,
    clearTimeout: () => {},
    fetch: async () => ({
      ok: true,
      status: 200,
      json: async () => ({}),
      text: async () => "",
    }),
    window: { addEventListener: () => {}, confirm: () => true },
    document: {
      getElementById: () => null,
      querySelector: () => null,
      querySelectorAll: () => [],
      createElement: () => ({
        style: {},
        classList: { add: () => {}, remove: () => {} },
        appendChild: () => {},
        remove: () => {},
      }),
      addEventListener: () => {},
    },
  };
  sandbox.globalThis = sandbox;
  vm.createContext(sandbox);
  vm.runInContext(source, sandbox, { filename: "dashboard.js" });
  const dash = sandbox.window.AemEdsDashboard;
  assert.ok(dash, "dashboard.js must expose window.AemEdsDashboard");
  return dash;
}

test("processBlockFiles classifies files into per-block buckets", () => {
  const dash = loadDashboard();
  const map = dash.processBlockFiles([
    { path: "blocks/hero/hero.js" },
    { path: "blocks/hero/hero.css" },
    { path: "blocks/hero/_hero.json" },
    { path: "blocks/hero/hero-example.html", sourcePath: "/content/wknd/hero" },
    { path: "blocks/hero/README.md" },
    { path: "blocks/nav/nav.html" },
    { path: "styles/base.css" }, // not under blocks/ — must be ignored
  ]);

  assert.ok(map.hero, "hero block entry exists");
  assert.equal(map.hero.name, "hero");
  assert.equal(map.hero.files.js.path, "blocks/hero/hero.js");
  assert.equal(map.hero.files.css.path, "blocks/hero/hero.css");
  assert.equal(map.hero.files.json.path, "blocks/hero/_hero.json");
  assert.equal(map.hero.files.demo.path, "blocks/hero/hero-example.html");
  assert.equal(map.hero.files.readme.path, "blocks/hero/README.md");
  assert.equal(map.hero.sourcePath, "/content/wknd/hero");
  assert.equal(map.hero.reconcile, "Created", "default reconcile state");

  assert.ok(map.nav, "nav block entry exists");
  assert.equal(map.nav.files.demo.path, "blocks/nav/nav.html");

  assert.equal(map.base, undefined, "non-block paths are ignored");
});

test("processBlockFiles handles empty and null input", () => {
  const dash = loadDashboard();
  // Note: objects returned from the vm sandbox carry another realm's
  // prototype, so we compare key sets rather than using deepEqual.
  assert.deepEqual(Object.keys(dash.processBlockFiles([])), []);
  assert.deepEqual(Object.keys(dash.processBlockFiles(null)), []);
});

test("applyReconcileBadges maps reconcile actions to labels", () => {
  const dash = loadDashboard();
  const map = dash.processBlockFiles([
    { path: "blocks/hero/hero.js" },
    { path: "blocks/footer/footer.js" },
  ]);

  dash.applyReconcileBadges([
    { message: "Block reconcile: hero=LEAVE footer=ENHANCE" },
  ]);
  assert.equal(map.hero.reconcile, "Left");
  assert.equal(map.footer.reconcile, "Enhanced");
});

test("applyReconcileBadges is case-insensitive and defaults to Created", () => {
  const dash = loadDashboard();
  const map = dash.processBlockFiles([{ path: "blocks/hero/hero.js" }]);

  dash.applyReconcileBadges([{ message: "block reconcile: hero=create" }]);
  assert.equal(map.hero.reconcile, "Created");
});

test("applyReconcileBadges uses the most recent matching event", () => {
  const dash = loadDashboard();
  const map = dash.processBlockFiles([{ path: "blocks/hero/hero.js" }]);

  dash.applyReconcileBadges([
    { message: "Block reconcile: hero=CREATE" },
    { message: "Block reconcile: hero=ENHANCE" },
  ]);
  assert.equal(map.hero.reconcile, "Enhanced");
});

test("applyReconcileBadges leaves state untouched without a match", () => {
  const dash = loadDashboard();
  const map = dash.processBlockFiles([{ path: "blocks/hero/hero.js" }]);

  dash.applyReconcileBadges([{ message: "unrelated event" }, null]);
  assert.equal(map.hero.reconcile, "Created");
});

test("escapeHtml neutralises markup characters", () => {
  const dash = loadDashboard();
  assert.equal(
    dash.escapeHtml('<b class="x">a & b</b>'),
    "&lt;b class=&quot;x&quot;&gt;a &amp; b&lt;/b&gt;",
  );
  assert.equal(dash.escapeHtml(""), "");
  assert.equal(dash.escapeHtml(null), "");
});
