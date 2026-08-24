package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.connectors.PlaywrightBrowserClient;
import com.adobe.aem.modernizer.mock.*;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.persistence.model.ValidationResultRecord;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectorsAndMocksTest {

    @Test
    void testMockAemClient() {
        MockAemClient client = new MockAemClient("https://mock-aem.local", "author", 42, true);
        assertThat(client.testConnection()).isTrue();
        assertThat(client.getAuthorUrl()).isEqualTo("https://mock-aem.local");
        assertThat(client.getRole()).isEqualTo("author");

        SiteInventory inv = client.crawl("/content/wknd", "/content/wknd/us/*");
        assertThat(inv).isNotNull();
        assertThat(inv.getPages()).isNotEmpty();

        MockAemClient defaultClient = new MockAemClient();
        assertThat(defaultClient.testConnection()).isTrue();
    }

    @Test
    void testMockEdsClient() {
        MockEdsClient client = new MockEdsClient("https://main--wknd--hlx.live");
        assertThat(client.testConnection()).isTrue();
        assertThat(client.getPreviewUrl("feature-1", "/us/en")).contains("/preview/feature-1/us/en");
        assertThat(client.getLiveUrl("/us/en")).contains("/live/us/en");
        assertThat(client.publish("/content/wknd/us/en")).isTrue();

        MockEdsClient defaultClient = new MockEdsClient();
        assertThat(defaultClient.getPreviewUrl(null, null)).isNotNull();
        assertThat(defaultClient.getLiveUrl(null)).isNotNull();
    }

    @Test
    void testMockGitHubClient() {
        MockGitHubClient client = new MockGitHubClient("https://github.com/company/wknd-eds");
        assertThat(client.testConnection()).isTrue();
        assertThat(client.getRepoUrl()).isEqualTo("https://github.com/company/wknd-eds");
        assertThat(client.branchExists("main")).isTrue();

        client.createBranch("feature/test");
        assertThat(client.branchExists("feature/test")).isTrue();
        assertThat(client.listBranches()).contains("feature/test");

        GeneratedFileRecord file = new GeneratedFileRecord("f1", "proj1", "job1", "blocks/teaser/teaser.js", "BLOCK_JS", "export default function() {}");
        client.commitFiles("feature/test", Collections.singletonList(file), "test commit");

        String prUrl = client.createPullRequest("PR Title", "PR Description", "feature/test", "main");
        assertThat(prUrl).contains("pull");

        MockGitHubClient defaultClient = new MockGitHubClient();
        assertThat(defaultClient.testConnection()).isTrue();
    }

    @Test
    void testMockBrowserClient() {
        MockBrowserClient client = new MockBrowserClient();
        assertThat(client.testConnection()).isTrue();

        ValidationResultRecord result = client.validatePage("https://main--wknd--hlx.live", "/content/wknd/us/en");
        assertThat(result.isPassed()).isTrue();
        assertThat(result.getVisualScore()).isGreaterThan(0.9);
        assertThat(result.getA11yScore()).isGreaterThan(0.9);
        assertThat(result.getScreenshotBase64()).isNotNull();

        assertThat(client.captureScreenshot("https://main--wknd--hlx.live")).isNotNull();
    }

    @Test
    void testPlaywrightBrowserClient() {
        PlaywrightBrowserClient client = new PlaywrightBrowserClient();
        assertThat(client.testConnection()).isTrue();

        ValidationResultRecord record = client.validatePage("https://main--wknd--hlx.live", "/content/wknd/us/en");
        assertThat(record).isNotNull();
        assertThat(record.getScreenshotBase64()).isNotNull();
        assertThat(client.captureScreenshot("http://localhost")).isNotNull();
    }
}
