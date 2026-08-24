package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.ssrf.UrlGuard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlGuardTest {

    @Test
    void testValidPublicUrls() {
        assertThatCode(() -> UrlGuard.validateUrl("https://github.com/company/wknd-eds", false))
                .doesNotThrowAnyException();
        assertThatCode(() -> UrlGuard.validateUrl("https://www.figma.com/file/12345", false))
                .doesNotThrowAnyException();
    }

    @Test
    void testLocalUrlsAllowedWhenConfigured() {
        assertThatCode(() -> UrlGuard.validateUrl("https://mock-aem.local/content/wknd", true))
                .doesNotThrowAnyException();
    }

    @Test
    void testPrivateIpBlockedInStrictMode() {
        assertThatThrownBy(() -> UrlGuard.validateUrl("http://127.0.0.1:8080/admin", false))
                .isInstanceOf(SecurityException.class);
    }
}
