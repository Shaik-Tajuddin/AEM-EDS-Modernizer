# AEM as a Cloud Service Deployment Runbook

## 1. Cloud Manager Deployment Architecture

The AEM-EDS-Modernizer codebase is 100% compliant with AEM as a Cloud Service (AEMaaCS) deployment requirements:

- **Targeted Maven Reactor**: Builds and packages OSGi bundles (`core`), immutable JCR configurations (`ui.config`), and mutable applications (`ui.apps`).
- **Service User Mapping**: Operates under the principal `modernizer-service` with strict sub-tree write permissions on `/var/modernizer`.
- **Stateless Pod Resilience**: Oak JCR nodes under `/var/modernizer/rag` are persistent across rolling pod recycles.
- **Git Trees Integration**: When deployed in Cloud Service environments where local disk access is restricted, `EDSRepositoryKnowledgeSource` utilizes `RealGitHubClient` to query the GitHub Git Trees API (`/repos/{owner}/{repo}/git/trees/{branch}?recursive=1`) and fetch document blobs via HTTPS.

---

## 2. OSGi Configurations (`ui.config`)

To configure the AI Gateway and Knowledge Source for Cloud Service environments:

### File: `ui.config/src/main/content/jcr_root/apps/aem-eds-modernizer/osgiconfig/config.author/com.adobe.aem.modernizer.ai.AiGateway.cfg.json`
```json
{
  "defaultProvider": "openai",
  "openaiApiKey": "$[secret:OPENAI_API_KEY]",
  "geminiApiKey": "$[secret:GEMINI_API_KEY]",
  "anthropicApiKey": "$[secret:ANTHROPIC_API_KEY]"
}
```

### File: `ui.config/src/main/content/jcr_root/apps/aem-eds-modernizer/osgiconfig/config.author/com.adobe.aem.modernizer.connectors.RealGitHubClient.cfg.json`
```json
{
  "githubToken": "$[secret:EDS_GITHUB_TOKEN]",
  "repoUrl": "https://github.com/my-org/my-eds-repo",
  "defaultBranch": "main"
}
```

---

## 3. Cloud Manager Secret Configuration

Configure Cloud Manager pipeline environment variables via Cloud Manager CLI or Web Console:

```bash
# Set OpenAI API Key secret
aio cloudmanager:set-environment-variables <ENVIRONMENT_ID> \
  --secret OPENAI_API_KEY "<your-openai-api-key>"

# Set GitHub Personal Access Token secret for EDS repo indexing
aio cloudmanager:set-environment-variables <ENVIRONMENT_ID> \
  --secret EDS_GITHUB_TOKEN "<your-github-pat>"
```

---

## 4. Targeted Maven Deployment Commands

Per project guidelines in `AGENTS.md`, execute targeted module deployments during testing:

- **Core Bundle Changes**:
  ```bash
  mvn clean install -Pdeploy -pl core -DskipTests
  ```
- **Dashboard UI & Clientlibs**:
  ```bash
  mvn clean install -Pdeploy -pl ui.apps -DskipTests
  ```
- **OSGi Configurations**:
  ```bash
  mvn clean install -Pdeploy -pl ui.config -DskipTests
  ```
- **Full Cloud Manager Emulation Build**:
  ```bash
  mvn clean install -Pdeploy -DskipTests
  ```
