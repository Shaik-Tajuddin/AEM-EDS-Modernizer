package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.EdsClient;
import org.osgi.service.component.annotations.Component;

/**
 * Mock EDS Client returning preview and live URL endpoints.
 */
@Component(service = EdsClient.class, immediate = true)
public class MockEdsClient implements EdsClient {

    private String edsBaseUrl = "https://eds-mock.local";

    public MockEdsClient() {}

    public MockEdsClient(String edsBaseUrl) {
        this.edsBaseUrl = edsBaseUrl;
    }

    @Override
    public boolean testConnection() {
        return true;
    }

    @Override
    public String getPreviewUrl(String branch, String path) {
        String cleanBranch = (branch != null) ? branch : "main";
        String cleanPath = (path != null && !path.startsWith("/")) ? "/" + path : (path != null ? path : "");
        return edsBaseUrl + "/preview/" + cleanBranch + cleanPath;
    }

    @Override
    public String getLiveUrl(String path) {
        String cleanPath = (path != null && !path.startsWith("/")) ? "/" + path : (path != null ? path : "");
        return edsBaseUrl + "/live" + cleanPath;
    }

    @Override
    public boolean publish(String path) {
        return true;
    }
}
