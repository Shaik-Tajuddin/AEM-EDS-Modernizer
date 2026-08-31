package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.scopes.MarkerEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScopesTest {

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
