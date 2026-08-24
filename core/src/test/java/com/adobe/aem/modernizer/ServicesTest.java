package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.BenchmarkSampleRecord;
import com.adobe.aem.modernizer.persistence.model.ClarificationRecord;
import com.adobe.aem.modernizer.persistence.model.DependencyEdgeRecord;
import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.persistence.model.UrlRedirectRecord;
import com.adobe.aem.modernizer.services.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServicesTest {

    @Test
    void testBenchmarkService() {
        BenchmarkService service = new BenchmarkService();

        BenchmarkSampleRecord sample1 = new BenchmarkSampleRecord("s1", "proj-1", "job-1", "connection", "test", 100L, 10.0);
        BenchmarkSampleRecord sample2 = new BenchmarkSampleRecord("s2", "proj-1", "job-1", "discovery", "crawl", 200L, 20.0);
        BenchmarkSampleRecord sample3 = new BenchmarkSampleRecord("s3", "proj-1", "job-1", "discovery", "crawl", 300L, 30.0);

        List<BenchmarkService.BenchmarkStats> statsList = service.computeStats(Arrays.asList(sample1, sample2, sample3));
        assertThat(statsList).hasSize(2);

        BenchmarkService.BenchmarkStats stats = statsList.get(0);
        assertThat(stats.getAgent()).isNotNull();
        assertThat(stats.getSampleCount()).isGreaterThan(0);
        assertThat(stats.getP50DurationMs()).isGreaterThan(0);
        assertThat(stats.getP95DurationMs()).isGreaterThan(0);
        assertThat(stats.getAvgCostMicros()).isGreaterThanOrEqualTo(0.0);

        assertThat(service.computeStats(null)).isEmpty();
        assertThat(service.computeStats(Collections.emptyList())).isEmpty();
    }

    @Test
    void testClarificationService() {
        Store store = new InMemoryStore();
        ClarificationService service = new ClarificationService(store);

        ClarificationRecord r1 = service.ask("proj-1", "job-1", "Which block?", "Rationale", Arrays.asList("Option A", "Option B"), "Option A");
        assertThat(r1).isNotNull();
        assertThat(r1.getOptions()).contains("Option A");

        service.resolve(r1.getId(), "Option B");

        ClarificationService emptyService = new ClarificationService();
        emptyService.resolve("dummy", "opt");
    }

    @Test
    void testImageDiffEngine() {
        ImageDiffEngine engine = new ImageDiffEngine();

        assertThat(engine.compare(null, null)).isEqualTo(0.0);
        assertThat(engine.compare(new byte[0], new byte[0])).isEqualTo(0.0);

        byte[] img1 = new byte[]{10, 20, 30, 40};
        byte[] img2 = new byte[]{10, 20, 30, 40};
        byte[] img3 = new byte[]{50, 60, 70, 80};

        assertThat(engine.compare(img1, img2)).isEqualTo(1.0);
        assertThat(engine.compare(img1, img3)).isLessThan(1.0);
    }

    @Test
    void testUrlRedirectServiceAndDependencyGraphService() {
        UrlRedirectService urlService = new UrlRedirectService();
        DependencyGraphService depService = new DependencyGraphService();

        SiteInventory inv = MockDataFactory.createWkndInventory("/content/wknd", null, 5);

        List<UrlRedirectRecord> redirects = urlService.buildRedirects("p1", "j1", inv);
        assertThat(redirects).isNotEmpty();

        List<DependencyEdgeRecord> edges = depService.buildGraph("p1", "j1", inv);
        assertThat(edges).isNotEmpty();

        assertThat(urlService.buildRedirects("p1", "j1", null)).isEmpty();
        assertThat(depService.buildGraph("p1", "j1", null)).isEmpty();
    }
}
