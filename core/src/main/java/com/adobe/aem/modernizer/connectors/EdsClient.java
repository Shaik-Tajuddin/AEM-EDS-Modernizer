package com.adobe.aem.modernizer.connectors;

/**
 * Connector interface for interacting with Edge Delivery Services (EDS).
 */
public interface EdsClient {
    boolean testConnection();
    String getPreviewUrl(String branch, String path);
    String getLiveUrl(String path);
    boolean publish(String path);
}
