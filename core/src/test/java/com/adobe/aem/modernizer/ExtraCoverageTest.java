package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.ai.ChatRequest;
import com.adobe.aem.modernizer.ai.ModelCapability;
import com.adobe.aem.modernizer.ai.TokenUsage;
import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.scopes.ScopeEvaluator;
import com.adobe.aem.modernizer.services.UrlRedirectService;
import com.adobe.aem.modernizer.ssrf.UrlGuard;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtraCoverageTest {

    @Test
    void testUrlGuardExhaustive() throws Exception {
        assertThatThrownBy(() -> UrlGuard.validateUrl(null, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UrlGuard.validateUrl("", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UrlGuard.validateUrl("ftp://example.com", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UrlGuard.validateUrl("http://", false)).isInstanceOf(IllegalArgumentException.class);

        // Local domains
        UrlGuard.validateUrl("http://localhost:8080", true);
        UrlGuard.validateUrl("http://mock.local", true);
        UrlGuard.validateUrl("http://mock.test", true);

        assertThatThrownBy(() -> UrlGuard.validateUrl("http://localhost:8080", false)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> UrlGuard.validateUrl("http://mock.local", false)).isInstanceOf(SecurityException.class);

        // IP restriction checks
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("10.0.0.1"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("172.16.0.1"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("172.31.255.255"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("192.168.1.1"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("0.0.0.0"))).isTrue();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("8.8.8.8"))).isFalse();
        assertThat(UrlGuard.isRestrictedIp(InetAddress.getByName("1.1.1.1"))).isFalse();
    }

    @Test
    void testTokenUsageAndChatRequestAndModelCapability() {
        TokenUsage tu = new TokenUsage();
        tu.setPromptTokens(50);
        tu.setCompletionTokens(50);
        assertThat(tu.getPromptTokens()).isEqualTo(50);
        assertThat(tu.getCompletionTokens()).isEqualTo(50);
        assertThat(tu.getTotalTokens()).isEqualTo(100);

        ChatRequest cr = new ChatRequest();
        cr.setAgentName("agent");
        cr.setPrompt("prompt");
        assertThat(cr.getAgentName()).isEqualTo("agent");
        assertThat(cr.getPrompt()).isEqualTo("prompt");

        ModelCapability mc = new ModelCapability();
        mc.setProvider("mock");
        mc.setModelName("mock-1");
        mc.setMaxContextTokens(4096);
        mc.setCapabilities(new HashSet<>(Arrays.asList("chat", "code")));
        assertThat(mc.getProvider()).isEqualTo("mock");
        assertThat(mc.getModelName()).isEqualTo("mock-1");
        assertThat(mc.getMaxContextTokens()).isEqualTo(4096);
        assertThat(mc.has("chat")).isTrue();
        assertThat(mc.has("code")).isTrue();
        assertThat(mc.has("vision")).isFalse();
    }

    @Test
    void testMarkerAndScopeEvaluators() {
        MarkerEvaluator marker = new MarkerEvaluator("edsModernize", "true");
        assertThat(marker.getMarkerProperty()).isEqualTo("edsModernize");
        assertThat(marker.getMarkerValue()).isEqualTo("true");

        assertThat(marker.isEligible(Collections.singletonMap("edsModernize", "true"), null, null)).isTrue();
        assertThat(marker.isEligible(Collections.singletonMap("edsModernize", "false"), null, null)).isFalse();
        assertThat(marker.isEligible(Collections.emptyMap(), null, null)).isFalse();
        assertThat(marker.isEligible(null, null, null)).isFalse();

        // Project overrides
        assertThat(marker.isEligible(Collections.singletonMap("customFlag", "yes"), "customFlag", "yes")).isTrue();
        assertThat(marker.isEligible(Collections.singletonMap("customFlag", "anyVal"), "customFlag", "*")).isTrue();
        assertThat(marker.isEligible(Collections.singletonMap("customFlag", "wrongVal"), "customFlag", "yes")).isFalse();

        ScopeEvaluator scope = new ScopeEvaluator();
        assertThat(scope.isInScope("/content/wknd/us/en", "/content/wknd", "/content/wknd/*")).isTrue();
        assertThat(scope.isInScope("/content/dam/asset.jpg", "/content/wknd", "/content/wknd/*")).isFalse();
        assertThat(scope.isInScope(null, "/content/wknd", "*")).isFalse();
        assertThat(scope.isInScope("/content/wknd/us/en", "/content/wknd", null)).isTrue();
        assertThat(scope.isInScope("/content/wknd/us/en", null, "")).isTrue();
        assertThat(scope.isInScope("/content/wknd/us/en", "/content/wknd", "/content/wknd/us*")).isTrue();
        assertThat(scope.isInScope("/content/wknd/us/en", "/content/other", null)).isFalse();
    }

    @Test
    void testUrlRedirectServiceDuplicatesAndNulls() {
        UrlRedirectService service = new UrlRedirectService();
        SiteInventory inv = MockDataFactory.createWkndInventory("/content/wknd", null, 5);

        // Introduce a duplicate path for conflict testing
        SiteInventory.PageInfo p1 = new SiteInventory.PageInfo("/content/wknd/us/en/page-1", "P1", "tpl");
        SiteInventory.PageInfo p2 = new SiteInventory.PageInfo("/content/wknd/us/en/page-1", "P2", "tpl");
        inv.getPages().add(p1);
        inv.getPages().add(p2);

        assertThat(service.buildRedirects("p1", "j1", inv)).isNotEmpty();
        assertThat(service.buildRedirects("p1", "j1", new SiteInventory())).isEmpty();
    }

    @Test
    void testSiteInventoryExhaustive() {
        SiteInventory inv = new SiteInventory();
        inv.setProjectId("proj1");
        inv.setJobId("job1");
        inv.setTotalPages(10);
        inv.setEligiblePages(8);
        inv.setExcludedPages(2);
        inv.setTimestamp(1000L);
        inv.setFigmaTokens(Collections.singletonMap("color", "#fff"));

        assertThat(inv.getProjectId()).isEqualTo("proj1");
        assertThat(inv.getJobId()).isEqualTo("job1");
        assertThat(inv.getTotalPages()).isEqualTo(10);
        assertThat(inv.getEligiblePages()).isEqualTo(8);
        assertThat(inv.getExcludedPages()).isEqualTo(2);
        assertThat(inv.getTimestamp()).isEqualTo(1000L);
        assertThat(inv.getFigmaTokens()).containsEntry("color", "#fff");
        assertThat(inv.pages()).isEqualTo(8);
        assertThat(inv.components()).isEqualTo(0);
        assertThat(inv.distinctBlocks()).isEqualTo(0);
        assertThat(inv.figmaFiles()).isEqualTo(1);

        SiteInventory.PageInfo page = new SiteInventory.PageInfo();
        page.setPath("/p");
        page.setTitle("T");
        page.setTemplate("/tpl");
        page.setEligible(false);
        page.setExclusionReason("Excluded by filter");
        page.setComponentResourceTypes(Collections.singletonList("comp"));
        page.setAssetPaths(Collections.singletonList("asset"));

        assertThat(page.getPath()).isEqualTo("/p");
        assertThat(page.getTitle()).isEqualTo("T");
        assertThat(page.getTemplate()).isEqualTo("/tpl");
        assertThat(page.isEligible()).isFalse();
        assertThat(page.getExclusionReason()).isEqualTo("Excluded by filter");
        assertThat(page.getComponentResourceTypes()).contains("comp");
        assertThat(page.getAssetPaths()).contains("asset");

        inv.setPages(Collections.singletonList(page));
        assertThat(inv.getPages()).hasSize(1);

        SiteInventory.ComponentInfo comp = new SiteInventory.ComponentInfo();
        comp.setResourceType("core/wcm/comp");
        comp.setTitle("Comp");
        comp.setGroup("General");
        comp.setOccurrenceCount(5);
        comp.setProposedEdsBlock("comp-block");
        comp.setCapabilityClassification("SUPPORTED");

        assertThat(comp.getResourceType()).isEqualTo("core/wcm/comp");
        assertThat(comp.getTitle()).isEqualTo("Comp");
        assertThat(comp.getGroup()).isEqualTo("General");
        assertThat(comp.getOccurrenceCount()).isEqualTo(5);
        assertThat(comp.getProposedEdsBlock()).isEqualTo("comp-block");
        assertThat(comp.getCapabilityClassification()).isEqualTo("SUPPORTED");

        inv.setComponents(Collections.singletonList(comp));
        assertThat(inv.getComponents()).hasSize(1);

        SiteInventory.TemplateInfo tpl = new SiteInventory.TemplateInfo();
        tpl.setPath("/conf/wknd/settings/wcm/templates/page-template");
        tpl.setTitle("Page Template");
        tpl.setAllowedComponents(Collections.singletonList("core/wcm/comp"));
        assertThat(tpl.getPath()).isEqualTo("/conf/wknd/settings/wcm/templates/page-template");
        assertThat(tpl.getTitle()).isEqualTo("Page Template");
        assertThat(tpl.getAllowedComponents()).contains("core/wcm/comp");
        inv.setTemplates(Collections.singletonList(tpl));
        assertThat(inv.getTemplates()).hasSize(1);

        SiteInventory.AssetInfo asset = new SiteInventory.AssetInfo();
        asset.setPath("/content/dam/wknd/asset.jpg");
        asset.setMimeType("image/jpeg");
        asset.setResolvable(true);
        assertThat(asset.getPath()).isEqualTo("/content/dam/wknd/asset.jpg");
        assertThat(asset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset.isResolvable()).isTrue();
        inv.setAssets(Collections.singletonList(asset));
        assertThat(inv.getAssets()).hasSize(1);

        SiteInventory.ContentFragmentInfo cf = new SiteInventory.ContentFragmentInfo();
        cf.setPath("/content/dam/cf/item");
        cf.setTitle("Item");
        cf.setModel("/conf/model");
        assertThat(cf.getPath()).isEqualTo("/content/dam/cf/item");
        assertThat(cf.getTitle()).isEqualTo("Item");
        assertThat(cf.getModel()).isEqualTo("/conf/model");
        inv.setContentFragments(Collections.singletonList(cf));
        assertThat(inv.getContentFragments()).hasSize(1);

        SiteInventory.MsmLiveCopyInfo msm = new SiteInventory.MsmLiveCopyInfo();
        msm.setSourcePath("/content/wknd/language-masters/en");
        msm.setLiveCopyPath("/content/wknd/us/en");
        msm.setRollOutConfig("/etc/msm/rolloutconfigs/default");
        assertThat(msm.getSourcePath()).isEqualTo("/content/wknd/language-masters/en");
        assertThat(msm.getLiveCopyPath()).isEqualTo("/content/wknd/us/en");
        assertThat(msm.getRollOutConfig()).isEqualTo("/etc/msm/rolloutconfigs/default");
        inv.setLiveCopies(Collections.singletonList(msm));
        assertThat(inv.getLiveCopies()).hasSize(1);
    }

    @Test
    void testDashboardApiNoArgAndApiRouterAdditionalEndpoints() throws Exception {
        com.adobe.aem.modernizer.dashboard.servlets.DashboardApi apiNoArg = new com.adobe.aem.modernizer.dashboard.servlets.DashboardApi();
        org.apache.sling.api.SlingHttpServletRequest req = org.mockito.Mockito.mock(org.apache.sling.api.SlingHttpServletRequest.class);
        org.apache.sling.api.SlingHttpServletResponse resp = org.mockito.Mockito.mock(org.apache.sling.api.SlingHttpServletResponse.class);
        
        apiNoArg.doGet(req, resp);
        org.mockito.Mockito.verify(resp).sendError(503, "ApiRouter not initialized");

        // Test ApiRouter subroutes via route()
        com.adobe.aem.modernizer.persistence.InMemoryStore store = new com.adobe.aem.modernizer.persistence.InMemoryStore();
        com.adobe.aem.modernizer.dashboard.ApiRouter router = new com.adobe.aem.modernizer.dashboard.ApiRouter(store, null);

        // Create project first
        com.adobe.aem.modernizer.persistence.model.ProjectRecord proj = new com.adobe.aem.modernizer.persistence.model.ProjectRecord("p-sub", "Sub Project", "http://localhost:4502", "/content/wknd", "https://github.com/a/b");
        store.saveProject(proj);

        // Test repairs endpoint
        String repairs = router.route("GET", "/projects/p-sub/repairs", null, null);
        assertThat(repairs).isNotNull();

        // Test benchmarks endpoint
        String benchmarks = router.route("GET", "/projects/p-sub/benchmarks", null, null);
        assertThat(benchmarks).isNotNull();

        // Test clarifications endpoint
        String clarifications = router.route("GET", "/projects/p-sub/clarifications", null, null);
        assertThat(clarifications).isNotNull();

        // Test rollout-stages endpoint
        String rollouts = router.route("GET", "/projects/p-sub/rollout-stages", null, null);
        assertThat(rollouts).isNotNull();

        // Test dependencies endpoint
        String deps = router.route("GET", "/projects/p-sub/dependencies", null, null);
        assertThat(deps).isNotNull();
    }
}
