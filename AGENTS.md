# Local Environment Context
- **AEM Author URL**: `http://localhost:4502`
- **AEM Publish URL**: `http://localhost:4503`
- **Default Credentials**: `admin:admin` (Base64: `YWRtaW46YWRtaW4=`)

# Targeted Maven Deployment (Performance Optimization)
Whenever you modify files, do NOT run a full reactor build unless cross-module dependencies changed. Target specific modules for faster deployment:
- **Core Java Changes only**: `mvn clean install -Pdeploy -pl core -DskipTests`
- **Dashboard UI / Component Changes only** (HTL, Clientlibs, CSS, JS under `ui.apps`): `mvn clean install -Pdeploy -pl ui.apps -DskipTests`
- **OSGi Configurations only** (under `ui.config`): `mvn clean install -Pdeploy -pl ui.config -DskipTests`
- **Shared content package only** (under `ui.content`): `mvn clean install -Pdeploy -pl ui.content -DskipTests`
- **Fallback (Multi-module changes)**: `mvn clean install -Pdeploy -DskipTests`

# UI & Clientlib Rules
- **No Double Inclusion**: When editing HTL/HTML files, never manually link JS or CSS files that are already loaded via AEM clientlib categories (e.g., `<sly data-sly-call="${clientlib.js @ categories='...'}">`).
- **ES6 Scope**: Avoid declaring variables globally in clientlib scripts to prevent collisions if the script is loaded in broader scopes.

# Fast Verification (Performance Optimization)
- Avoid using the headless browser subagent just to test REST API responses or servlet routing. Instead, execute rapid programmatic checks via curl or inline node scripts.

# Tool Usage Rule
- Always utilize MCP servers (especially the lazy-loaded AEM tools like `executeJCRQuery` or `searchContent`) and agent skills whenever they are required or applicable for the task at hand.

# Browser Access Rule
- Do not include screenshots during browser access.

# AEM Development Best Practices
- **Skills & MCP Utilization**: Always check and use available skills from the `.agents/skills` folder and utilize required MCP servers for specific development workflows, migrations, and AEM tasks.
- **AEM Core Components**: Prefer extending AEM Core Components over building custom components from scratch whenever possible.
- **Sling Models**: Use Sling Models (Java) for business logic and data preparation rather than putting logic in HTL/JSP.
- **HTL (Sightly)**: Use HTL for all component rendering. Keep logic out of HTL; it should be strictly used for presentation.
- **Clientlibs**: Organize CSS and JS into clientlibs with appropriate categories, namespaces, and dependencies to optimize loading and prevent collisions.
- **Configurations**: Keep environment-specific configurations in OSGi configs instead of hardcoding values.
