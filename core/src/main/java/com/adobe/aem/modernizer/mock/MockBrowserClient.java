package com.adobe.aem.modernizer.mock;

import com.adobe.aem.modernizer.connectors.BrowserClient;
import com.adobe.aem.modernizer.persistence.model.ValidationResultRecord;
import org.osgi.service.component.annotations.Component;

import java.util.UUID;

/**
 * Mock Browser Client returning high-quality validation passes and deterministic base64 screenshots.
 */
@Component(service = BrowserClient.class, immediate = true)
public class MockBrowserClient implements BrowserClient {

    @Override
    public boolean testConnection() {
        return true;
    }

    @Override
    public ValidationResultRecord validatePage(String url, String targetPath) {
        ValidationResultRecord record = new ValidationResultRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTargetPath(targetPath != null ? targetPath : url);
        record.setValidationType("VISUAL_A11Y");
        record.setPassed(true);
        record.setVisualScore(0.97);
        record.setA11yScore(0.99);
        record.setScreenshotBase64(captureScreenshot(url));
        return record;
    }

    @Override
    public String captureScreenshot(String url) {
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    }
}
