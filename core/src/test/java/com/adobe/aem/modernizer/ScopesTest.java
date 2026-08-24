package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import com.adobe.aem.modernizer.scopes.ScopeEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScopesTest {

    @Test
    void testScopeEvaluator() {
        ScopeEvaluator evaluator = new ScopeEvaluator();

        assertThat(evaluator.isInScope(null, "/content/wknd", null)).isFalse();
        assertThat(evaluator.isInScope("/content/wknd/us/en", "/content/wknd", null)).isTrue();
        assertThat(evaluator.isInScope("/content/wknd", "/content/wknd", null)).isTrue();
        assertThat(evaluator.isInScope("/content/other", "/content/wknd", null)).isFalse();

        // Scope filters: wildcard /*, * and exact path
        assertThat(evaluator.isInScope("/content/wknd/us/en/page", "/content/wknd", "/content/wknd/us/*")).isTrue();
        assertThat(evaluator.isInScope("/content/wknd/ca/en/page", "/content/wknd", "/content/wknd/us/*")).isFalse();
        assertThat(evaluator.isInScope("/content/wknd/us/en", "/content/wknd", "/content/wknd/us*")).isTrue();
        assertThat(evaluator.isInScope("/content/wknd/us/en", "/content/wknd", "/content/wknd/us/en")).isTrue();
        assertThat(evaluator.isInScope("/content/wknd/us/en/sub", "/content/wknd", "/content/wknd/us/en")).isTrue();
        assertThat(evaluator.isInScope("/content/wknd/us/fr", "/content/wknd", "/content/wknd/us/en")).isFalse();
    }

    @Test
    void testMarkerEvaluator() {
        MarkerEvaluator evaluator = new MarkerEvaluator("edsModernize", "true");

        assertThat(evaluator.isEligible(null, null, null)).isFalse();

        Map<String, Object> props = new HashMap<>();
        props.put("edsModernize", "true");
        props.put("status", "ACTIVE");

        assertThat(evaluator.isEligible(props, null, null)).isTrue();
        assertThat(evaluator.isEligible(props, "status", "ACTIVE")).isTrue();
        assertThat(evaluator.isEligible(props, "status", "INACTIVE")).isFalse();
        assertThat(evaluator.isEligible(Collections.emptyMap(), null, null)).isFalse();

        MarkerEvaluator wildcardEvaluator = new MarkerEvaluator("marker", "*");
        props.put("marker", "any-value");
        assertThat(wildcardEvaluator.isEligible(props, null, null)).isTrue();

        MarkerEvaluator defaultEvaluator = new MarkerEvaluator();
        assertThat(defaultEvaluator).isNotNull();
    }
}
