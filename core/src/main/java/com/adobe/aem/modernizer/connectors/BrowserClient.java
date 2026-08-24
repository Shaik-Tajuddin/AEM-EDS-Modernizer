package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.ValidationResultRecord;

/**
 * Connector interface for browser rendering and validation (visual + a11y).
 */
public interface BrowserClient {
    boolean testConnection();
    ValidationResultRecord validatePage(String url, String targetPath);
    String captureScreenshot(String url);
}
