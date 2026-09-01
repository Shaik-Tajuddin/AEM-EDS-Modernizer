package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.SiteInventory;

/**
 * Connector interface for interacting with AEM Author / Publish instances.
 */
public interface AemClient {
    boolean testConnection();
    SiteInventory crawl(String contentRoot, String pageScope);
    default SiteInventory crawl(String contentRoot, String pageScope, String scopeMode) {
        return crawl(contentRoot, pageScope);
    }
    String getAuthorUrl();
    String getRole(); // "author" or "publish"
}
