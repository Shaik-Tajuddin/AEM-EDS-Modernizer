package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.agents.AgentContext;
import com.adobe.aem.modernizer.connectors.PipelineHealLoop;
import com.adobe.aem.modernizer.connectors.PipelineHealRepairs;
import com.adobe.aem.modernizer.mock.MockGitHubClient;
import com.adobe.aem.modernizer.persistence.InMemoryStore;
import com.adobe.aem.modernizer.persistence.model.GeneratedFileRecord;
import com.adobe.aem.modernizer.persistence.model.JobRecord;
import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineHealLoopTest {

    @Test
    void escapeJsonStringEscapesControlCharacters() {
        String escaped = PipelineHealRepairs.escapeJsonString("Meet our guides.\n\"Quoted\"\tpath\\end");
        String json = "{\"text\":\"" + escaped + "\"}";
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = JsonUtil.fromJson(json, Map.class);
        assertThat(parsed.get("text")).isEqualTo("Meet our guides.\n\"Quoted\"\tpath\\end");
    }

    @Test
    void sanitizeBlockJsonFixesRawNewlinesInStrings() throws Exception {
        String broken = "{\n  \"text\": \"<p>Meet our extraordinary travel guides\nand friends</p>\"\n}\n";
        String sanitized = PipelineHealRepairs.sanitizeBlockJson(broken);
        JsonUtil.mapper().readTree(sanitized);
        assertThat(sanitized).contains("\\n").doesNotContain("guides\nand");
    }

    @Test
    void mergeSectionFilterKeepsExistingAndAppendsOnce() throws Exception {
        String section = "{ \"filters\": [ { \"id\": \"section\", \"components\": [\"text\", \"image\", \"hero\"] } ] }";
        String once = PipelineHealRepairs.mergeSectionFilter(section, List.of("text", "experiencefragment", "hero"));
        String twice = PipelineHealRepairs.mergeSectionFilter(once, List.of("experiencefragment"));
        assertThat(countId(once, "experiencefragment")).isEqualTo(1);
        assertThat(countId(twice, "experiencefragment")).isEqualTo(1);
        assertThat(countId(once, "text")).isEqualTo(1);
        assertThat(countId(once, "image")).isEqualTo(1);
    }

    @Test
    void sanitizeGeneratedJsFixesJcrIdentifiersAndUselessReturn() {
        String js = "import {\n  getHtmlFromBlockRow,\n  getTextFromBlockRow,\n} from 'x';\n"
                + "function extractConfig(block) {\n  return { jcr:title: getTextFromBlockRow(rows[1]) };\n}\n"
                + "function appendEvents(config) {\n  if (!config?.mainEl) return;\n}\n";
        String fixed = PipelineHealRepairs.sanitizeGeneratedJs(js);
        assertThat(fixed).contains("jcrTitle").doesNotContain("jcr:title");
        assertThat(fixed).doesNotContain("getHtmlFromBlockRow");
        assertThat(fixed).contains("function appendEvents() {");
        assertThat(fixed).doesNotContain("if (!config?.mainEl) return;");
    }

    @Test
    void mergeComponentListAppendsMissingIds() {
        String list = "{ \"components\": [\"text\", \"image\"] }";
        String merged = PipelineHealRepairs.mergeComponentList(list, List.of("image", "experiencefragment"));
        assertThat(merged).contains("experiencefragment");
        assertThat(countId(merged, "image")).isEqualTo(1);
    }

    @Test
    void healLoopSanitizesJsonRegistersSectionAndDispatchesNpm() throws Exception {
        HealMock gh = new HealMock();
        gh.createBranch("feat/heal-proj");
        String broken = "{\n  \"definitions\": [],\n  \"models\": [],\n  \"text\": \"<p>Meet our extraordinary travel guides\nand friends</p>\"\n}\n";
        gh.commitFiles("feat/heal-proj", List.of(
                rec("blocks/text/_text.json", broken),
                rec("blocks/experiencefragment/_experiencefragment.json", "{ \"id\": \"experiencefragment\" }"),
                rec("models/_section.json", "{ \"filters\": [ { \"id\": \"section\", \"components\": [\"text\", \"image\"] } ] }")
        ), "seed");

        InMemoryStore store = new InMemoryStore();
        ProjectRecord project = new ProjectRecord("heal-proj", "Heal", "http://localhost:4502",
                "/content/wknd", "https://github.com/company/wknd-eds");
        project.setMaxRepairAttempts(3);
        JobRecord job = new JobRecord("job-heal", "heal-proj", "PREVIEWING");
        store.saveProject(project);
        store.saveJob(job);

        PipelineHealLoop.start(gh, new AgentContext(project, job), store, null);

        String textJson = gh.getFileContent("feat/heal-proj", "blocks/text/_text.json");
        JsonUtil.mapper().readTree(textJson);
        assertThat(textJson).contains("\\n");

        String section = gh.getFileContent("feat/heal-proj", "models/_section.json");
        assertThat(section).contains("experiencefragment");
        assertThat(countId(section, "text")).isEqualTo(1);

        assertThat(gh.dispatched).containsExactly("lint:fix", "build:json");
        assertThat(job.getMetadata()).containsEntry("ciHeal", "passed");
    }

    private static int countId(String json, String id) {
        int count = 0;
        int idx = 0;
        String needle = "\"" + id + "\"";
        while ((idx = json.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static GeneratedFileRecord rec(String path, String content) {
        return new GeneratedFileRecord(path, "heal-proj", "job-heal", path, "BLOCK_JSON", content);
    }

    private static final class HealMock extends MockGitHubClient {
        private int polls;
        private final List<String> dispatched = new ArrayList<>();

        HealMock() {
            super("https://github.com/company/wknd-eds");
        }

        @Override
        public Map<String, Object> getLatestWorkflowRun(String branch) {
            polls++;
            Map<String, Object> run = new LinkedHashMap<>();
            run.put("runId", String.valueOf(polls));
            run.put("status", "completed");
            run.put("conclusion", polls == 1 ? "failure" : "success");
            run.put("htmlUrl", getRepoUrl() + "/actions");
            return run;
        }

        @Override
        public String getWorkflowRunLogs(String runId) {
            return "npm run build:json\nSyntaxError: Bad control character in string literal in JSON\n"
                    + "\"text\": \"<p>Meet our extraordinary travel guides\n";
        }

        @Override
        public Map<String, Object> dispatchWorkflow(String ref, String workflowFile, Map<String, String> inputs) {
            dispatched.add(inputs != null ? String.valueOf(inputs.getOrDefault("command", "")) : "");
            return super.dispatchWorkflow(ref, workflowFile, inputs);
        }
    }
}
