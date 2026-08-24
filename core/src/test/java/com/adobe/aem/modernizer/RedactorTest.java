package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.security.Redactor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactorTest {

    @Test
    void testSecretRedaction() {
        String input1 = "Connecting using token ghp_1234567890abcdef123456 to repo";
        assertThat(Redactor.redact(input1)).isEqualTo("Connecting using token [REDACTED] to repo");

        String input2 = "OpenAI key sk-123456789012345678901234 and Anthropic sk-ant-123456789012345678901234";
        assertThat(Redactor.redact(input2)).contains("[REDACTED]");

        String input3 = "Cloning https://admin:SuperSecret123@myhost.local/repo.git";
        assertThat(Redactor.redact(input3)).doesNotContain("SuperSecret123");
    }
}
