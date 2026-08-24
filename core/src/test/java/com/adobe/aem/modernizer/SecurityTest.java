package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.security.AuditService;
import com.adobe.aem.modernizer.security.RbacPolicy;
import com.adobe.aem.modernizer.security.Redactor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityTest {

    @Test
    void testRbacPolicy() {
        RbacPolicy rbac = new RbacPolicy();

        // System actors
        assertThat(rbac.isAllowed("admin", Collections.emptySet(), RbacPolicy.Permission.MIGRATE)).isTrue();
        assertThat(rbac.isAllowed("system", Collections.emptySet(), RbacPolicy.Permission.ADMIN)).isTrue();
        assertThat(rbac.isAllowed("modernizer-service", Collections.emptySet(), RbacPolicy.Permission.DRY_RUN)).isTrue();
        assertThat(rbac.isAllowed(null, Collections.emptySet(), RbacPolicy.Permission.READ)).isFalse();

        // Admin group
        assertThat(rbac.isAllowed("user1", new HashSet<>(Arrays.asList("modernizer-admins")), RbacPolicy.Permission.MIGRATE)).isTrue();
        assertThat(rbac.isAllowed("user1", new HashSet<>(Arrays.asList("administrators")), RbacPolicy.Permission.ADMIN)).isTrue();

        // Operator group
        assertThat(rbac.isAllowed("user2", new HashSet<>(Arrays.asList("modernizer-operators")), RbacPolicy.Permission.DRY_RUN)).isTrue();
        assertThat(rbac.isAllowed("user2", new HashSet<>(Arrays.asList("modernizer-operators")), RbacPolicy.Permission.READ)).isTrue();
        assertThat(rbac.isAllowed("user2", new HashSet<>(Arrays.asList("modernizer-operators")), RbacPolicy.Permission.MIGRATE)).isFalse();

        // Anonymous / no roles
        assertThat(rbac.isAllowed("user3", Collections.emptySet(), RbacPolicy.Permission.READ)).isTrue();
        assertThat(rbac.isAllowed("user3", Collections.emptySet(), RbacPolicy.Permission.MIGRATE)).isFalse();
        assertThat(rbac.isAllowed("user3", null, RbacPolicy.Permission.READ)).isTrue();
    }

    @Test
    void testAuditService() {
        Store store = new InMemoryStore();
        AuditService audit = new AuditService(store);

        audit.audit("proj-1", "job-1", "admin", "orchestrator", "Created project proj-1");
        audit.audit("proj-1", "job-1", "operator", "connection", "INFO", "CONNECTING", "DISCOVERING", "Connected to endpoints");

        assertThat(store.getEvents("job-1")).hasSize(2);

        AuditService emptyAudit = new AuditService();
        emptyAudit.audit("p1", "j1", "admin", "agent", "msg");
    }

    @Test
    void testRedactor() {
        assertThat(Redactor.redact(null)).isNull();
        assertThat(Redactor.redact("")).isEmpty();
        assertThat(Redactor.redact("normal log message")).isEqualTo("normal log message");

        String githubLog = "Connecting with token ghp_1234567890abcdef1234567890 to repo";
        assertThat(Redactor.redact(githubLog)).contains("[REDACTED]").doesNotContain("ghp_");

        String openaiLog = "OpenAI key sk-1234567890abcdef1234567890abcdef12345";
        assertThat(Redactor.redact(openaiLog)).contains("[REDACTED]").doesNotContain("sk-123456");

        String anthropicLog = "Anthropic key sk-ant-1234567890abcdef1234567890abcdef12345";
        assertThat(Redactor.redact(anthropicLog)).contains("[REDACTED]").doesNotContain("sk-ant-");

        String authHeader = "Authorization: Bearer token1234567890";
        assertThat(Redactor.redact(authHeader)).contains("[REDACTED]");

        String basicAuthUrl = "https://user:secretpassword@author.com/path";
        assertThat(Redactor.redact(basicAuthUrl)).contains("[REDACTED]");
    }
}
