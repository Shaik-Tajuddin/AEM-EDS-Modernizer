package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.AemClient;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import org.osgi.service.component.annotations.Component;

/**
 * Mock AEM Client returning deterministic WKND site inventories.
 */
@Component(service = AemClient.class, immediate = true)
public class MockAemClient implements AemClient {

    private String authorUrl = "https://mock-aem.local";
    private String role = "author";
    private int pageCount = 42;
    private boolean reachable = true;

    public MockAemClient() {}

    public MockAemClient(String authorUrl, String role, int pageCount, boolean reachable) {
        this.authorUrl = authorUrl;
        this.role = role;
        this.pageCount = pageCount;
        this.reachable = reachable;
    }

    @Override
    public boolean testConnection() {
        return reachable;
    }

    @Override
    public SiteInventory crawl(String contentRoot, String pageScope) {
        return crawl(contentRoot, pageScope, "RECURSIVE");
    }

    @Override
    public SiteInventory crawl(String contentRoot, String pageScope, String scopeMode) {
        int count = "SINGLE_PAGE".equalsIgnoreCase(scopeMode) ? 1 : pageCount;
        return MockDataFactory.createWkndInventory(contentRoot, pageScope, count);
    }

    @Override
    public String getAuthorUrl() {
        return authorUrl;
    }

    @Override
    public String getRole() {
        return role;
    }
}
