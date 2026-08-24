package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.AdvancedRepairAgent;
import com.adobe.aem.modernizer.agents.AgentContext;
import com.adobe.aem.modernizer.ai.AiGateway;
import com.adobe.aem.modernizer.mock.MockDataFactory;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.Store;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedRepairAgentTest {

    @Test
    void testAdvancedRepairAgentProducesRecordsAndAvoidsZeroRepairsBug() throws Exception {
        Store store = new InMemoryStore();
        AiGateway ai = new AiGateway();
        AdvancedRepairAgent agent = new AdvancedRepairAgent(store, ai, 5);

        ProjectRecord project = new ProjectRecord("proj-1", "Test", "https://mock-aem.local", "/content/wknd", "https://github.com/company/wknd-eds");
        JobRecord job = new JobRecord("job-1", "proj-1", "MIGRATE");
        store.saveProject(project);
        store.saveJob(job);

        AgentContext ctx = new AgentContext(project, job);
        ctx.setInventory(MockDataFactory.createWkndInventory("/content/wknd", null, 10));

        agent.execute(ctx);

        assertThat(store.getRepairAttempts("job-1")).isNotEmpty();
        assertThat(store.getRepairAttempts("job-1").size()).isGreaterThan(0);
        assertThat(store.getRepairAttempts("job-1").get(0).isSuccessful()).isTrue();
    }
}
