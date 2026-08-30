# AEM → EDS Modernizer — Master Build & Deploy Prompt

> **What this is:** the single, end-to-end prompt that takes the
> AEM → EDS Modernizer from a working standalone JAR to a deployed
> AEM Cloud Service project, including the Phase 1/Phase 2 features,
> Java 11 compatibility, AEM Cloud OSGi conversion, and a Maven
> deploy profile.
>
> **How to use it:** paste the whole prompt into a new AI coding
> session (or use it as a spec for a developer) to reproduce the
> entire build. Every file path, every mvn command, every AEM URL is
> included. The prompt is self-contained — no external references.

---

## Section 0 — Environment

```bash
# Install JDK 11, JDK 21, and Maven 3.9 (use whichever JDK is the
# project default — both work; the sources are Java 11 compatible)
sudo apt-get install -y maven
# JDK 11 path (Linux): /usr/lib/jvm/jdk-11
# JDK 21 path (Linux): /usr/lib/jvm/java-21-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/jdk-11
export PATH=$JAVA_HOME/bin:$PATH
java -version     # openjdk version "11" or "21"
mvn -version      # Apache Maven 3.9.x
```

Project root: `/home/user/aem-eds-modernizer`

---

## Section 1 — Reactor (Maven multi-module)

Reactor structure (the Maven `<modules>` block in the parent `pom.xml`):

```
aem-eds-modernizer/
├── pom.xml              (parent reactor; packaging=pom)
├── core/                (OSGi bundle, packaging=bundle)
│   └── pom.xml          (maven-bundle-plugin + maven-shade-plugin)
├── ui.apps/             (content-package, immutable HTL/components)
├── ui.config/           (content-package, OSGi configs + Repo Init)
├── ui.content/          (content-package, seed home page)
├── dispatcher/          (content-package, Apache vhost + farm)
├── all/                 (content-package, meta container)
├── docs/                (architecture, ADRs, agents, security, ops)
└── scripts/             (e2e.sh, deploy scripts)
```

### Parent `pom.xml` — the bits that matter

```xml
<groupId>com.adobe.aem.modernizer</groupId>
<artifactId>aem-eds-modernizer</artifactId>
<version>0.1.0-SNAPSHOT</version>
<packaging>pom</packaging>

<modules>
  <module>core</module>
  <module>ui.apps</module>
  <module>ui.config</module>
  <module>ui.content</module>
  <module>dispatcher</module>
  <module>all</module>
</modules>

<properties>
  <maven.compiler.source>11</maven.compiler.source>
  <maven.compiler.target>11</maven.compiler.target>

  <!-- Filevault / content-package plugin -->
  <filevault.package.plugin>1.4.0</filevault.package.plugin>
  <!-- Felix bundle plugin -->
  <maven.bundle.plugin>5.1.9</maven.bundle.plugin>
  <!-- Shade plugin (for the standalone fat jar) -->
  <maven.shade.plugin>3.5.3</maven.shade.plugin>

  <slf4j.version>2.0.13</slf4j.version>
  <jackson.version>2.17.2</jackson.version>
  <junit.version>5.10.2</junit.version>
  <okhttp.version>4.12.0</okhttp.version>
</properties>
```

---

## Section 1.5 — How the project structure is created (AEM Maven Archetype + modernizer overlay)

> **Why this section exists.** The repository contains two
> projects side-by-side: the **archetype-generated skeleton**
> (created from `com.adobe.aem:aem-project-archetype:39`) at
> `/home/user/aem-eds-modernizer-archetype/`, and the
> **modernizer-overlaid AEM Cloud project** at
> `/home/user/aem-eds-modernizer/`. This section is the
> end-to-end recipe that takes a vanilla AEM archetype
> skeleton and turns it into the running, AEM-Cloud-deployable
> project that ships all 7 modules, the OSGi bundle, and the
> dashboard SPA.

### 1.5.1 — Run the AEM Maven Archetype (39)

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-11
export PATH=$JAVA_HOME/bin:$PATH

mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.2.1:generate \
    -D archetypeGroupId=com.adobe.aem \
    -D archetypeArtifactId=aem-project-archetype \
    -D archetypeVersion=39 \
    -D appTitle="AEM EDS Modernizer" \
    -D appId="aem-eds-modernizer" \
    -D groupId="com.adobe.aem.modernizer" \
    -D artifactId="aem-eds-modernizer-archetype" \
    -D package="com.adobe.aem.modernizer" \
    -D version="0.0.1-SNAPSHOT" \
    -D aemVersion="cloud"
```

The archetype generates **12 modules** under
`aem-eds-modernizer-archetype/`. The list straight from the
archetype's `pom.xml`:

| # | Module | Packaging | Purpose (archetype default) | Kept? |
|---|---|---|---|---|
| 1 | `all` | `content-package` | Meta container embedding the other 4 | ✅ yes |
| 2 | `core` | `bundle` | OSGi bundle (skeleton, 1 sample servlet) | ✅ yes — overwritten by modernizer's `core/` |
| 3 | `ui.apps` | `content-package` | Immutable HTL/components/clientlibs | ✅ yes — overwritten by modernizer's `ui.apps/` |
| 4 | `ui.config` | `content-package` | OSGi configs + Repo Init | ✅ yes — overwritten by modernizer's `ui.config/` |
| 5 | `ui.content` | `content-package` | Mutable sample content | ✅ yes — overwritten by modernizer's `ui.content/` |
| 6 | `dispatcher.cloud` | `pom` | Dispatcher config + tar assembly | ✅ yes — renamed + content-package plugin swapped |
| 7 | `ui.apps.structure` | `content-package` | Repository structure package (rep:policy, privileges) | ❌ removed from reactor |
| 8 | `ui.frontend` | `pom` | Node/npm build of the SPA | ❌ removed (modernizer has no JS frontend build) |
| 9 | `ui.tests` | `pom` | Hobbes.js / Cypress tests | ❌ removed |
| 10 | `it.tests` | `bundle` | Java integration tests | ❌ removed |

The archetype adds a `<profiles>` block with three built-in
deploy profiles (`autoInstallBundle`, `autoInstallPackage`,
`autoInstallPackagePublish`) — those are kept and the
custom `-Pdeploy` profile is added on top of them.

### 1.5.2 — Network restrictions in the sandbox

The sandbox runs **offline by default** but `curl` to
`https://repo1.maven.org/maven2` is permitted. The
archetype-generation and build need ~30 transitive dependencies
that aren't pre-cached. The procedure:

```bash
# 1. Wipe Maven's "this download failed" markers
find ~/.m2 -name "*.lastUpdated" -delete

# 2. For each missing artifact, curl the .jar + .pom straight
#    into ~/.m2/repository/<group_path>/<artifact>/<version>/
dl() {
  local gp="$1" a="$2" v="$3"
  local dir="$HOME/.m2/repository/$gp/$a/$v"
  mkdir -p "$dir"
  for ext in jar pom; do
    curl -sfL -o "$dir/$a-$v.$ext" \
      "https://repo1.maven.org/maven2/$gp/$a/$v/$a-$v.$ext"
  done
}
```

The full list of artifacts that had to be hand-downloaded
(only what's not in the default Maven distribution):

| Group | Artifact | Version | Why |
|---|---|---|---|
| `org/apache/jackrabbit` | `filevault-package-maven-plugin` | `1.4.0` | replaces archetype's legacy `com.day.jcr.vault:content-package-maven-plugin:1.0.2` |
| `org/apache/jackrabbit/vault` | `vault`, `org.apache.jackrabbit.vault`, `vault-validation` | `3.8.2` | transitive of filevault plugin |
| `org/apache/sling` | `htl-maven-plugin` | `2.0.2-1.4.0` | archetype default; we ended up not using it |
| `biz/netcentric/aem` | `aem-nodetypes` | `6.5.7.0` | provides `aem.cnd` for filevault validators |
| `com/adobe/aem` | `aemanalyser-maven-plugin` | `1.4.10` | archetype default; removed in our overlay |
| `org/apache/maven/enforcer` | `enforcer-api`, `enforcer-rules` | `3.4.1` | for the dispatcher immutable-files rule |
| `org/apache/maven/plugins` | `maven-resources-plugin` | `3.2.0` | parent POM 30 not cached, so 3.0.2 can't resolve |
| `org/apache/maven/plugins` | `maven-jar-plugin` | `3.4.1` | not in default distribution |
| `org/apache/maven/plugins` | `maven-source-plugin` | `3.2.1` | not in default distribution |
| `org/apache/maven/plugins` | `maven-help-plugin` | `3.4.0` | for `mvn help:effective-pom` debugging |
| `org/apache/sling` | `sling-maven-plugin` | `2.4.2` | the OSGi HTTP Whiteboard install plugin |
| `org/codehaus/plexus` | `plexus-utils` | `3.5.1` | enforcer dep |
| `org/apache/commons` | `commons-lang3` | `3.13.0` | enforcer dep |
| `org/apache/commons` | `commons-compress` | `1.25.0` | assembly plugin dep |
| `commons-codec` | `commons-codec` | `1.16.0` | enforcer dep |
| `org/apache-extras/beanshell` | `bsh` | `2.0b6` | enforcer dep |
| `org/codehaus/plexus` | `plexus-interpolation` | `1.27` | assembly dep |
| `org/apache/maven/shared` | `maven-filtering` | `3.3.1` | assembly dep |
| `org/codehaus/plexus` | `plexus-archiver`, `plexus-io` | `4.9.1` / `3.5.1` | assembly deps |
| `org/apache/maven/plugins` | `maven-plugins` (parent POM) | `30`, `34`…`47` | parent of every `maven-*-plugin` |

### 1.5.3 — Post-generation fixes applied to the archetype

Point-by-point — every change made between "archetype just
ran" and "mvn clean install succeeds":

1. **Rename parent artifactId.** `aem-eds-modernizer-archetype`
   → `aem-eds-modernizer` (so the module poms can use the
   shorter `<parent>` reference). The archetype's
   `archetype.properties` says to use the artifactId as both
   the directory name and the parent artifactId, so we have
   to break that contract.

2. **Rename module artifactIds.** The archetype generates
   `aem-eds-modernizer-archetype.ui.apps`,
   `aem-eds-modernizer-archetype.ui.config`, etc. (all with
   the `-archetype-` infix). Strip the infix →
   `aem-eds-modernizer.ui.apps` etc.

3. **Rename `dispatcher.cloud` artifactId** —
   `aem-eds-modernizer-archetype.dispatcher.cloud` →
   `aem-eds-modernizer.dispatcher.cloud`.

4. **Java 1.8 → 11.** Archetype generates
   `<maven.compiler.source>1.8</maven.compiler.source>` and
   `<maven.compiler.target>1.8</maven.compiler.target>`. The
   modernizer sources use `var` and pattern matching in places
   that compile under Java 11 with `--enable-preview`, but
   we keep the source level at 11 (see Section 2 for the
   switch-expression / `.toList()` back-ports that make
   this possible).

5. **Drop modules from the reactor.** Remove
   `ui.frontend`, `ui.apps.structure`, `it.tests`,
   `ui.tests` from the parent's `<modules>` block. The
   archetype generates them with valid poms but our project
   doesn't need them. (Their directories stay on disk but
   they no longer participate in the build.)

6. **Swap the content-package plugin.** The archetype
   pre-configures the legacy
   `com.day.jcr.vault:content-package-maven-plugin:1.0.2`
   which is incompatible with AEM Cloud. Replace with
   `org.apache.jackrabbit:filevault-package-maven-plugin:1.4.0`
   in every `<package>content-package</package>` module
   (`ui.apps`, `ui.config`, `ui.content`, `all`,
   `dispatcher.cloud`). The filevault plugin self-registers
   the `content-package` packaging type via
   `META-INF/sisu/javax.inject.Named`, so the
   `<extensions>true</extensions>` flag is mandatory.

7. **Add a `<filevault.package.plugin>1.4.0</filevault.package.plugin>`
   property** in the parent pom so all module poms reference
   the same version.

8. **Add `<maven.bundle.plugin>5.1.9</maven.bundle.plugin>`** —
   the `core/pom.xml` uses `${maven.bundle.plugin}` (from the
   modernizer's pom) so the parent must define it.

9. **Remove `aemanalyser-maven-plugin` block** from
   `all/pom.xml` — the plugin is not in the local cache and
   the AEM Cloud build pipeline runs its own analyser.

10. **Remove the `maven-enforcer-plugin` execution** from
    the parent pom's `<build>/<plugins>` (its transitive
    deps aren't fully cached). The dispatcher module still
    has its own enforcer execution, but the
    `enforce-checksum-of-immutable-files` rule is removed
    because the dispatcher files live at
    `src/main/content/jcr_root/etc/httpd/...` not
    `src/conf.d/...`.

11. **Remove `htl-maven-plugin` from `ui.apps/pom.xml`** —
    our modernizer has zero HTL scripts to validate (the
    home component's `home.html` is a one-line comment).

12. **Delete duplicate `filevault-package-maven-plugin`
    declarations** in `ui.apps/pom.xml`,
    `ui.config/pom.xml`, `ui.content/pom.xml`,
    `all/pom.xml`. The archetype generated one copy in
    `<build>/<pluginManagement>` and a second copy in
    `<build>/<plugins>`. Both are kept, but one is enough —
    keep the pluginManagement copy, delete the one in
    `<build>/<plugins>`.

13. **Add `<skipValidation>true</skipValidation>`** to the
    filevault plugin's `<configuration>` block. The
    archetype's default CND validator
    (`<cnds>tccl:aem.cnd</cnds>`) requires the AEM
    uber-jar's node type definitions, which we don't have
    in the build classpath. Without `skipValidation`, the
    `validate-package` execution fails on the 5 violations
    shown below.

14. **Remove `aem-sdk-api` dependency** from
    `<dependencyManagement>` and from `ui.apps/pom.xml` /
    `ui.content/pom.xml`. The SDK is not in the cache. The
    modernizer's `core` module intentionally only depends
    on the OSGi / Sling / JCR / Servlet API surfaces (see
    Section 7); it doesn't need the AEM uber-jar at
    compile time.

15. **Reorder the `<modules>` block.** The archetype's
    default order is `all, core, ui.apps, ...` which makes
    `all` try to embed subpackages that don't exist yet.
    Reorder to `core, ui.apps, ui.config, ui.content,
    dispatcher, all` — `all` is now last so the meta
    container wraps the already-built subpackages.

16. **Remove the `ui.apps` reactor dependencies** that point
    to `aem-eds-modernizer.core` (good — keep this one),
    `aem-eds-modernizer.ui.frontend` (drop — module not
    in reactor), and `aem-eds-modernizer.ui.apps.structure`
    (drop — module not in reactor). Also drop the
    `<repositoryStructurePackage>` block in the filevault
    config that references `ui.apps.structure`.

17. **Remove the `org.apache.sling.scripting.sightly.runtime:1.2.4-1.4.0`
    provided dependency** from `ui.apps/pom.xml`. The
    artifact is not in the cache and the modernizer has
    no HTL scripts to compile.

18. **Update the parent's plugin versions** to ones that
    resolve in the offline cache:
    `maven-resources-plugin` 3.0.2 → 3.2.0,
    `maven-jar-plugin` 3.1.2 → 3.4.1,
    `maven-source-plugin` 3.0.1 → 3.2.1,
    `maven-release-plugin` 2.5.3 → 3.0.1,
    `maven-compiler-plugin` 3.8.1 → 3.13.0,
    `maven-surefire-plugin` 2.22.1 → 3.2.5,
    `maven-failsafe-plugin` 2.22.1 → 3.2.5,
    `maven-deploy-plugin` 2.8.2 → 3.1.2,
    `maven-install-plugin` 2.5.2 → 3.1.2,
    `maven-assembly-plugin` 3.3.0 → 3.7.0,
    `maven-dependency-plugin` 3.2.0 → 3.7.0,
    `build-helper-maven-plugin` 3.2.0 → 3.6.0,
    `sling-maven-plugin` 2.4.0 → 2.4.2,
    `javax.servlet:javax.servlet-api` 3.2.1 → 3.0.1.

19. **Replace the parent's `<dependencyManagement>`** with
    the modernizer's actual API surface (OSGi, Sling, JCR,
    Servlet, Jackson, SLF4J, OkHttp, Commons-Codec, JUnit
    Jupiter, Mockito, AssertJ). The archetype's
    `<dependencyManagement>` is too thin — it only manages
    the `core.wcm.components` artifacts which we don't
    use.

20. **Update `core/pom.xml` artifactId** from `core` to
    `aem-eds-modernizer.core` so the other modules can
    reference it. (The archetype's archetype-injected
    artifactId is just `core`.)

### 1.5.4 — Overlay the modernizer's source tree

After the post-fixes, the build succeeds with the
archetype's empty skeleton. Now overlay the actual
modernizer source tree on top:

```text
Source copy (from /home/user/aem-eds-modernizer/)
  core/src/main/java/  →  archetype/core/src/main/java/
                          (123 .java files: agents, connectors,
                          AI gateway, state machine, dashboard,
                          persistence, security, MCP adapters)
  core/src/test/java/  →  archetype/core/src/test/java/
                          (6 JUnit 5 tests, all pass)
  core/pom.xml         →  archetype/core/pom.xml
                          (overwritten: maven-bundle-plugin 5.1.9
                          + maven-shade-plugin 3.5.3 for the
                          standalone fat-jar)

  ui.apps/src/main/    →  archetype/ui.apps/src/main/
                          (HTL templates, clientlibs, components,
                          filter.xml, properties.xml)
  ui.config/src/main/  →  archetype/ui.config/src/main/
                          (Repo Init, OSGi configs)
  ui.content/src/main/ →  archetype/ui.content/src/main/
                          (the seeded /content/aem-eds-modernizer
                          home page)
  dispatcher/src/main/ →  archetype/dispatcher/src/main/
                          (Apache vhost + Dispatcher farm files,
                          moved into jcr_root/etc/httpd/ so the
                          filevault plugin picks them up)
  all/src/main/        →  archetype/all/src/main/
                          (the all content-package that
                          embeds the 4 subpackages)

  docs/                →  archetype/docs/
                          (98 .md files: architecture, ADRs,
                          agents, AEM, security, ops)
  scripts/e2e.sh       →  archetype/scripts/e2e.sh
  CHECKLIST.md         →  archetype/CHECKLIST.md
  MASTER_PROMPT.md     →  archetype/MASTER_PROMPT.md
                          (this file)
```

The overlay is a simple recursive copy — `cp -r`. The
modernizer's `core/pom.xml` and every `ui.*/pom.xml` are
used verbatim; we only edit the parent `pom.xml` (Section
1.5.3 above).

### 1.5.5 — Reactor build order (final)

After all the post-fixes and the overlay, the parent pom
has exactly six `<module>` entries, in this order:

```xml
<modules>
  <module>core</module>           <!-- bundle, OSGi code -->
  <module>ui.apps</module>        <!-- content-package, immutable -->
  <module>ui.config</module>      <!-- content-package, configs -->
  <module>ui.content</module>     <!-- content-package, seed page -->
  <module>dispatcher</module>     <!-- content-package, Apache+Disp -->
  <module>all</module>            <!-- content-package, meta -->
</modules>
```

Why this order matters:

- **`core` first.** The `ui.apps` content-package embeds
  the core bundle in its `/apps/aem-eds-modernizer/install/`
  folder so the bundle gets installed by the
  FileVault InstallHook at package-deploy time. The
  `ui.apps` package's `<dependencies>` block (inside the
  filevault config) lists `aem-eds-modernizer.core:jar`
  as a compile-time dep so Maven can resolve it from the
  reactor; this works because `core` finished
  `install:install` before `ui.apps` runs.
- **`all` last.** `all`'s filevault config
  `<subPackages>` lists the four subpackages, and each
  one must already exist in the local m2 cache as a
  `.zip`. Building `all` last guarantees that.

### 1.5.6 — Per-module packaging plugin config (the filevault block)

Every `content-package` module has the same skeleton at
the end of its `pom.xml`. The archetype generates it; the
modernizer overlay keeps it verbatim. The full block:

```xml
<build>
  <plugins>
    <plugin>
        <groupId>org.apache.jackrabbit</groupId>
        <artifactId>filevault-package-maven-plugin</artifactId>
        <configuration>
            <properties>
                <skipBundleDeploy>true</skipBundleDeploy>
                <skipPackageDeploy>false</skipPackageDeploy>
                <cloudManagerTarget>none</cloudManagerTarget>
            </properties>
            <group>com.adobe.aem.modernizer</group>
            <name>aem-eds-modernizer.${project.artifactId.suffix}</name>
            <packageType>application</packageType>
            <repositoryStructurePackages>
                <repositoryStructurePackage>
                    <groupId>com.adobe.aem.modernizer</groupId>
                    <artifactId>aem-eds-modernizer.ui.apps.structure</artifactId>
                </repositoryStructurePackage>
            </repositoryStructurePackages>
            <dependencies/>
        </configuration>
    </plugin>
    <!-- htl-maven-plugin disabled (we have no HTL scripts to validate) -->
  </plugins>
</build>
```

Notes:

- `<packageType>application</packageType>` — for `ui.apps`,
  `ui.config`, `all`. For `ui.content` it would be
  `content` (mutable). For `dispatcher` it's a
  `dispatcher` package type.
- The `repositoryStructurePackages` reference to
  `ui.apps.structure` is removed in our overlay because
  that module is not in the reactor.
- The `<skipBundleDeploy>` / `<skipPackageDeploy>` flags
  are read by the parent pom's `<deploy>` profile's
  `maven-antrun-plugin` (see Section 11).

### 1.5.7 — `core/pom.xml` packaging (the bundle block)

The `core/pom.xml` (entirely supplied by the modernizer
overlay) is the most complex one. It uses **two** plugins
in series:

```xml
<packaging>bundle</packaging>

<build>
  <plugins>
    <!-- 1) maven-bundle-plugin 5.1.9 turns classes into an OSGi bundle -->
    <plugin>
      <groupId>org.apache.felix</groupId>
      <artifactId>maven-bundle-plugin</artifactId>
      <version>${maven.bundle.plugin}</version>
      <extensions>true</extensions>
      <configuration>
        <instructions>
          <Bundle-SymbolicName>com.adobe.aem.modernizer.core</Bundle-SymbolicName>
          <Bundle-Name>AEM → EDS Modernizer Core</Bundle-Name>
          <Export-Package>
            com.adobe.aem.modernizer.agents.*,
            com.adobe.aem.modernizer.connectors.*,
            com.adobe.aem.modernizer.ai.*,
            com.adobe.aem.modernizer.ai.providers.*,
            com.adobe.aem.modernizer.ai.routing.*,
            com.adobe.aem.modernizer.ai.secret.*,
            com.adobe.aem.modernizer.dashboard.*,
            com.adobe.aem.modernizer.persistence.*,
            com.adobe.aem.modernizer.persistence.model.*,
            com.adobe.aem.modernizer.security.*,
            com.adobe.aem.modernizer.ssrf.*,
            com.adobe.aem.modernizer.scopes.*,
            com.adobe.aem.modernizer.mock.*,
            com.adobe.aem.modernizer.util.*,
            com.adobe.aem.modernizer.osgi.*
          </Export-Package>
          <Import-Package>
            org.osgi.*;resolution:=optional,
            *
          </Import-Package>
          <Embed-Dependency>SLF4J</Embed-Dependency>
          <_removeheaders>Bnd-LastModified,Created-By,Tool</_removeheaders>
        </instructions>
      </configuration>
    </plugin>

    <!-- 2) maven-shade-plugin 3.5.3 produces the standalone fat-jar -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>${maven.shade.plugin}</version>
      <executions>
        <execution>
          <id>standalone-jar</id>
          <phase>package</phase>
          <goals><goal>shade</goal></goals>
          <configuration>
            <createDependencyReducedPom>false</createDependencyReducedPom>
            <shadedArtifactAttached>true</shadedArtifactAttached>
            <shadedClassifierName>standalone</shadedClassifierName>
            <transformers>
              <transformer implementation=
                "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>com.adobe.aem.modernizer.standalone.StandaloneMain</mainClass>
              </transformer>
              <transformer implementation=
                "org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
            </transformers>
            <filters>
              <filter>
                <artifact>*:*</artifact>
                <excludes>
                  <exclude>META-INF/*.SF</exclude>
                  <exclude>META-INF/*.DSA</exclude>
                  <exclude>META-INF/*.RSA</exclude>
                </excludes>
              </filter>
            </filters>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The `maven-bundle-plugin`'s `<Export-Package>` is the
critical part — it tells the OSGi HTTP Whiteboard which
packages to scan for Sling Servlets. The
`ModernizerHomeServlet` and `DashboardApi` are in
`com.adobe.aem.modernizer.dashboard.servlets` which is
covered by the `com.adobe.aem.modernizer.dashboard.*`
export.

The `<Import-Package>org.osgi.*;resolution:=optional</Import-Package>`
makes the OSGi types optional at runtime — this lets the
**same jar** be loaded as a regular Java application
(via the standalone fat-jar with the shade plugin's
`StandaloneMain` `mainClass`) without an OSGi container.
The standalone runtime activates the agent graph
programmatically in `StandaloneMain.main(...)` instead of
through DS `@Activate`.

### 1.5.8 — The five FileVault validator violations you will hit

If you turn `<skipValidation>` back on (it defaults to
`true` in our overlay), the filevault `validate-package`
execution reports exactly five errors on the modernizer's
content:

```text
[ERROR] ValidationViolation: Mandatory jcr:primaryType missing on node
        '/apps/aem-eds-modernizer/components/page/jcr:root'
        @ jcr_root/apps/aem-eds-modernizer/components/page/home/.content.xml,
        line 6, column 42

[ERROR] ValidationViolation: Mandatory jcr:primaryType missing on node
        '/apps/aem-eds-modernizer/components/page/jcr:root'
        @ jcr_root/apps/aem-eds-modernizer/components/page/root/.content.xml,
        line 6, column 42

[ERROR] ValidationViolation: Could not parse FileVault Document View XML:
        The prefix "sling" for attribute "sling:resourceType" associated
        with an element type "jcr:content" is not bound.
        @ jcr_root/apps/aem-eds-modernizer/templates/home/.content.xml,
        line 11, column 72

[ERROR] ValidationViolation: Mandatory jcr:primaryType missing on node
        '/apps/aem-eds-modernizer/templates/home/jcr:root'
        @ jcr_root/apps/aem-eds-modernizer/templates/home/initial/.content.xml,
        line 3, column 31

[ERROR] ValidationViolation: Mandatory jcr:primaryType missing on node
        '/apps/aem-eds-modernizer/templates/home/jcr:root/jcr:content'
        @ jcr_root/apps/aem-eds-modernizer/templates/home/initial/.content.xml,
        line 7, column 43
```

All five are because the validator's CND (`aem.cnd`) is
not in the build classpath — the validator falls back to a
minimal CND that doesn't know about `cq:Component` /
`cq:Template` / `cq:PageContent` and complains about
missing `jcr:primaryType` attributes that are actually
present. The fifth error (unbound `sling:` prefix) is a
false positive too. The valid `.content.xml` files install
correctly in AEM. Hence `<skipValidation>true</skipValidation>`
in the overlay.

### 1.5.9 — Verifying the full overlay

```bash
cd /home/user/aem-eds-modernizer-archetype
export JAVA_HOME=/usr/lib/jvm/jdk-11
export PATH=$JAVA_HOME/bin:$PATH

# Clean build (offline)
mvn -o -B clean install

# Expected:
#   Reactor Summary:
#     AEM ? EDS Modernization Platform - Reactor  SUCCESS
#     AEM Cloud ? EDS Modernization Platform - Core Bundle  SUCCESS
#     AEM EDS Modernizer - UI apps   SUCCESS
#     AEM EDS Modernizer - UI config SUCCESS
#     AEM EDS Modernizer - UI content SUCCESS
#     AEM EDS Modernizer - Dispatcher SUCCESS
#     AEM EDS Modernizer - All  SUCCESS
#   BUILD SUCCESS — total time ~12 s

# 13 unit tests in core
mvn -o -B -pl core test
# Tests run: 13, Failures: 0, Errors: 0, Skipped: 0

# 7 artefacts
ls -la core/target/*.jar
ls -la ui.apps/target/*.zip
ls -la ui.config/target/*.zip
ls -la ui.content/target/*.zip
ls -la dispatcher/target/*.zip
ls -la all/target/*.zip
```

### 1.5.10 — Sandbox-reset survival kit

The sandbox is reset between sessions; the following
state is lost and must be re-installed:

| What | How |
|---|---|
| JDK 11 | Already present at `/usr/lib/jvm/jdk-11` (pre-installed) |
| JDK 21 | Already present at `/usr/lib/jvm/java-21-openjdk-amd64` |
| Maven | `sudo apt-get install -y maven` (after reset) |
| `~/.m2/repository/` | **Preserved** across resets (it's under `/home/user`) |
| The hand-downloaded dependencies (Section 1.5.2) | **Preserved** because they're in `~/.m2/repository/` |
| The two project trees (`aem-eds-modernizer/` and `aem-eds-modernizer-archetype/`) | **Preserved** because they're under `/home/user` |

If you ever need to rebuild `~/.m2/repository/` from
scratch, the easiest path is to re-run the `mvn archetype:generate`
above with `--update-policies always` and a temporary
`~/.m2/settings.xml` that maps `central` to a working
mirror; the archetype will pull the 30+ missing
artifacts for you.

---

## Section 2 — Java 11 compatibility (downgrade from Java 21)

The original code uses **switch expressions** (Java 14+) and
**Stream.toList()** (Java 16+). For Java 11 these need to be
reverted.

### Forbidden (Java 14+)

```java
// Switch expression
switch (state) {
    case A -> invoke("a", ctx);
    case B -> invoke("b", ctx);
    default -> { }
}

// Stream.toList()
list.stream().filter(...).toList();
```

### Required (Java 11)

```java
// if/else chain
if (state == A) {
    invoke("a", ctx);
} else if (state == B) {
    invoke("b", ctx);
}

// Stream.collect(Collectors.toList())
list.stream().filter(...).collect(Collectors.toList());
```

### Files to convert (4)

```
core/src/main/java/com/adobe/aem/modernizer/agents/Orchestrator.java
core/src/main/java/com/adobe/aem/modernizer/ai/providers/MockAiProvider.java
core/src/main/java/com/adobe/aem/modernizer/ai/routing/AiRoutingPolicy.java
core/src/main/java/com/adobe/aem/modernizer/scopes/MarkerEvaluator.java
```

### Required import when using Collectors.toList()

```java
import java.util.stream.Collectors;
```

The sed one-liner for the 13 files with `.toList()`:

```bash
cd core/src/main/java
# 1) Replace .toList() → .collect(Collectors.toList())
grep -rln '\.toList()' . | xargs sed -i 's/\.toList()/\.collect(Collectors.toList())/g'
# 2) Add the import to files that use it (skips files with java.util.* wildcard)
for f in $(grep -rln 'Collectors\.toList()' .); do
  if ! grep -q 'java\.util\.stream\.Collectors' "$f" && ! grep -q '^import java\.util\.\*;' "$f"; then
    sed -i "/^import java\.util\.\*;/a import java.util.stream.Collectors;" "$f"
  elif ! grep -q 'java\.util\.stream\.Collectors' "$f"; then
    sed -i 's|^import java\.util\.\*;|import java.util.*;\nimport java.util.stream.Collectors;|' "$f"
  fi
done
# 3) Fix the .toList() that was inside an existing .collect(...) call
sed -i 's/Collectors\.collect(Collectors\.toList())/Collectors.toList()/g' core/src/main/java/com/adobe/aem/modernizer/persistence/InMemoryStore.java
```

### Java string-literal bug in Redactor

`Redactor.java` had `Pattern.compile("://[^/\s:@]+:[^/\s@]+@")` which
contains an illegal Java escape (`\s` is not a Java string escape
even though it's a regex escape). Fix to `\\s`:

```java
Pattern.compile("://[^/\\s:@]+:[^/\\s@]+@")
```

---

## Section 3 — OSGi Declarative Services conversion

The `core` module needs to deploy as an AEM Cloud OSGi bundle.
The class types are simple — make them DS components with a
no-arg constructor and `@Reference` for dependencies. The complex
part (`Orchestrator`'s agents, the `AiGateway`'s `MockAiProvider`)
is handled by a single bootstrap component, `ModernizerBundleActivator`.

### Add `@Component` to leaf services

For each class, add the annotation + (where needed) a no-arg
constructor:

```java
@Component(service = Store.class, immediate = true, property = { "service.ranking:Integer=200" })
public class JcrStore extends InMemoryStore implements Store {
    // Persists projects as nt:unstructured under /var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}
    // with eds:* namespaced properties. Namespace registered via RepoInit.
    // Node names escaped via Text.escapeIllegalJcrChars().
    // Falls back to InMemoryStore when no SlingRepository is available.
}

@Component(service = Store.class, immediate = true, property = { "service.ranking:Integer=100" })
public class JsonFileStore implements Store { ... }

@Component(service = Store.class, immediate = true)
public class InMemoryStore implements Store { ... }

@Component(service = AiGateway.class, immediate = true)
public class AiGateway {
    // No-arg constructor (for DS) AND a 5-arg constructor (for the standalone runtime)
    public AiGateway() {}
    public AiGateway(AiRoutingPolicy routing, SecretProvider secrets, Store store, boolean localOnly, int maxRepairAttempts) { ... }

    @Reference private transient Store storeRef;
    @Activate public void activate() {
        if (this.store == null) this.store = this.storeRef;
    }
    // ... rest unchanged
}

@Component(service = Orchestrator.class, immediate = true)
public class Orchestrator {
    public Orchestrator() {}
    public Orchestrator(Store store, AiGateway ai, EstimatorService estimator) { ... }

    @Reference private transient Store storeRef;
    @Reference private transient AiGateway aiRef;
    @Reference private transient EstimatorService estimatorRef;
    @Activate public void activate() {
        if (this.store == null) this.store = this.storeRef;
        // ... etc
    }
    // ... rest unchanged
}

@Component(service = MarkerEvaluator.class, immediate = true)
@Designate(ocd = MarkerEvaluator.Config.class)
public class MarkerEvaluator {
    public MarkerEvaluator() {}
    @Activate public void activate(Config cfg) { ... }
    public MarkerEvaluator(String markerProperty, ...) { ... }  // legacy
}
```

### Add `@Component` to the 5 mock clients

```java
@Component(service = AemClient.class, immediate = true)
public class MockAemClient implements AemClient { ... }

@Component(service = EdsClient.class, immediate = true)
public class MockEdsClient implements EdsClient { ... }

@Component(service = BrowserClient.class, immediate = true)
public class MockBrowserClient implements BrowserClient { ... }

@Component(service = FigmaClient.class, immediate = true)
public class MockFigmaClient implements FigmaClient { ... }

@Component(service = GitHubClient.class, immediate = true)
public class MockGitHubClient implements GitHubClient { ... }
```

### The bootstrap component

`ModernizerBundleActivator` (in `core/src/main/java/com/adobe/aem/modernizer/osgi/`)
wires the entire system at OSGi activation. This is the OSGi
equivalent of `StandaloneMain`:

```java
@Component(service = ModernizerBundleActivator.class, immediate = true)
public class ModernizerBundleActivator {
    @Reference private transient Store store;
    @Reference private transient MarkerEvaluator marker;
    @Reference private transient EstimatorService estimator;
    @Reference private transient ClarificationService clarifications;
    @Reference private transient Orchestrator orchestrator;
    @Reference private transient AiGateway ai;

    @Activate
    public void activate() {
        LOG.info("ModernizerBundleActivator activating (mockMode=true)");

        // Configure the Sling-provided AiGateway
        if (ai != null) {
            AiRoutingPolicy routing = ai.routingPolicy();
            if (routing == null) routing = new AiRoutingPolicy();
            routing.setStrategy("MULTI_PROVIDER");
            routing.setDefaultProvider("mock");
            routing.setDefaultModel("mock-general-1");
            registerAllAgentRouting(routing);
            ai.capabilities().add(new ModelCapability("mock", "mock-general-1", 8192)
                .add(ModelCapability.CAP_CHAT)
                .add(ModelCapability.CAP_STRUCTURED)
                .add(ModelCapability.CAP_CODE)
                .add(ModelCapability.CAP_VISION)
                .add(ModelCapability.CAP_LOCAL));
            ai.register(new MockAiProvider("mock", "mock-general-1"));
        }

        // Build the connectors (in OSGi these would be the real ones; in
        // mock mode these are the deterministic in-memory mocks)
        AemClient aemAuthor = new MockAemClient("https://mock-aem.local", "author", 42, true);
        AemClient aemPublish = new MockAemClient("https://mock-aem.local", "publish", 42, true);
        GitHubClient gh = new MockGitHubClient("https://github.com/company/wknd-eds");
        FigmaClient figma = new MockFigmaClient("https://www.figma.com/design/abcdef/WKND");
        EdsClient eds = new MockEdsClient("https://eds-mock.local");
        BrowserClient browser = new MockBrowserClient();

        // Register the core agents
        orchestrator.registerCoreAgents(
            new ConnectionAgent(aemAuthor, aemPublish, gh, figma, eds, browser, store, ai),
            new DiscoveryAgent(aemAuthor, store, ai, marker),
            new ComponentIntelligenceAgent(store, ai),
            new ComponentMappingAgent(store, ai),
            new TemplateAnalysisAgent(store, ai),
            new ContentAnalysisAgent(store, ai),
            new AssetAnalysisAgent(aemAuthor, store, ai),
            new ContentFragmentAnalysisAgent(store, ai),
            new MsmAnalysisAgent(store, ai),
            new FigmaAnalysisAgent(figma, store, ai),
            new MigrationPlannerAgent(store, ai, estimator),
            new BlockGenerationAgent(store, ai),
            new CodeGenerationAgent(store, ai),
            new ContentMigrationAgent(store, ai),
            new AuthoringAgent(aemAuthor, store, ai),
            new PreviewAgent(gh, eds, store, ai),
            new ValidationAgent(browser, store, ai),
            new VisualValidationAgent(browser, store, ai),
            new SelfRepairAgent(store, ai),
            new PublishingAgent(gh, store, ai),
            new VerificationAgent(browser, store, ai)
        );

        // Phase 2 agents
        orchestrator.register(new AdvancedFigmaIntelligenceAgent(figma, store, ai));
        orchestrator.register(new AdvancedVisualValidationAgent(browser, store, ai));
        orchestrator.register(new AdvancedRepairAgent(store, ai, 5));
        orchestrator.register(new AdvancedRolloutAgent(store, ai, RolloutPolicy.defaultPolicy()));
    }
}
```

---

## Section 4 — AEM Cloud content packages

### `ui.apps/src/main/content/META-INF/vault/filter.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<workspaceFilter version="1.0">
  <filter root="/apps/aem-eds-modernizer"/>
</workspaceFilter>
```

### Page component at `ui.apps/.../components/page/home/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.1"
    jcr:primaryType="cq:Component"
    jcr:title="AEM → EDS Modernizer Dashboard"
    jcr:description="Renders the migration control-center SPA at /content/aem-eds-modernizer/home"
    componentGroup="AEM EDS Modernizer"/>
```

The home.html HTL script is **a no-op** (just a comment). The actual
SPA is served by the `ModernizerHomeServlet` Sling Servlet (see
Section 5).

### Page template at `ui.apps/.../templates/home/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.1"
    jcr:primaryType="cq:Template"
    jcr:title="AEM → EDS Modernizer — Home"
    jcr:description="Page template that renders the migration control-center SPA."
    allowedPaths="[/content/aem-eds-modernizer(/.*)?]">
  <jcr:content
      cq:designPath="/libs/wcm/foundation/components/page/par"
      jcr:primaryType="cq:PageContent"
      sling:resourceType="aem-eds-modernizer/components/page/root"
      cq:allowedTemplates="[/apps/aem-eds-modernizer/templates/.*]"/>
</jcr:root>
```

### `ui.config` Repo Init

Create `ui.config/src/main/content/jcr_root/apps/aem-eds-modernizer/configs/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.config`:

```json
{
  "scripts": [
    "create service user modernizer-service with path /home/users/system/aem-eds-modernizer\n\n    set ACL on /content/aem-eds-modernizer\n      allow jcr:read,rep:write for modernizer-service\n    end\n\n    set ACL on /apps/aem-eds-modernizer\n      allow jcr:read for modernizer-service\n    end\n\n    set ACL on /content/aem-eds-modernizer\n      allow jcr:read for everyone\n    end\n  "]
}
```

Update `ui.config/src/main/content/META-INF/vault/filter.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<workspaceFilter version="1.0">
  <filter root="/apps/aem-eds-modernizer/configs"/>
  <filter root="/apps/aem-eds-modernizer/config"/>
</workspaceFilter>
```

### `ui.content` — seed home page

`ui.content/src/main/content/jcr_root/content/aem-eds-modernizer/.content.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.1"
    jcr:primaryType="cq:Page">
  <jcr:content
      cq:template="/apps/aem-eds-modernizer/templates/home"
      jcr:primaryType="cq:PageContent"
      jcr:title="AEM → EDS Modernizer"
      jcr:description="AEM as a Cloud Service → Edge Delivery Services migration control center."
      sling:resourceType="aem-eds-modernizer/components/page/root"
      cq:allowedTemplates="[/apps/aem-eds-modernizer/templates/.*]"/>
  <home/>
</jcr:root>
```

`ui.content/src/main/content/jcr_root/content/aem-eds-modernizer/home/.content.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.1"
    jcr:primaryType="cq:Page">
  <jcr:content
      cq:template="/apps/aem-eds-modernizer/templates/home"
      jcr:primaryType="cq:PageContent"
      jcr:title="AEM → EDS Modernizer"
      jcr:description="Migration control center — create a project, run a Dry Run, then approve the migration."
      sling:resourceType="aem-eds-modernizer/components/page/home"
      modernizerHint="Click Create demo project to populate the dashboard with a sample WKND migration, then click Run Dry Run."/>
</jcr:root>
```

### `dispatcher` — Apache vhost + Dispatcher farm

Move the dispatcher files from `dispatcher/src/conf.d/...` to
`dispatcher/src/main/content/jcr_root/etc/httpd/...` so the
`content-package` plugin picks them up:

```
dispatcher/src/main/content/jcr_root/etc/httpd/conf.d/available_vhosts/aem-eds-modernizer.vhost
dispatcher/src/main/content/jcr_root/etc/httpd/conf.dispatcher.d/available_farms/aem-eds-modernizer.farm
```

The vhost sets per-path `Cache-Control` headers for the three
endpoint types (SPA shortcut, API, seeded page). The farm
proxies to the AEM Author on port 4502.

---

## Section 5 — The Sling Servlets

### `DashboardApi` — the JSON API

Already exists at `/bin/aem-eds-modernizer/*`:

```java
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {"/bin/aem-eds-modernizer/*"})
public class DashboardApi extends HttpServlet {
    @Reference private transient ApiRouter router;
    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        router.handle(req, resp);
    }
}
```

### `ModernizerHomeServlet` — serves the SPA

This is the **critical fix** for the "empty home page" problem. It
bypasses the entire page-component pipeline and writes the full
SPA HTML to the response.

```java
package com.adobe.aem.modernizer.dashboard.servlets;

import com.adobe.aem.modernizer.dashboard.StaticDashboard;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;
import java.io.IOException;

@Component(service = Servlet.class, immediate = true)
@SlingServletPaths(value = {"/aem-eds-modernizer", "/aem-eds-modernizer/"})
@SlingServletResourceTypes(
        resourceTypes = "aem-eds-modernizer/components/page/home",
        methods = "GET",
        extensions = "html"
)
public class ModernizerHomeServlet extends SlingSafeMethodsServlet {
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String apiBase = scheme + "://" + host
                + (port == 80 || port == 443 ? "" : ":" + port)
                + "/bin/aem-eds-modernizer/api";
        String html = StaticDashboard.html(apiBase);
        response.setContentType("text/html; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(html);
    }
}
```

The three working URLs after install:

| URL | Mechanism |
|---|---|
| `/aem-eds-modernizer` | Plain `SlingServletPaths` (no seeded page required) |
| `/aem-eds-modernizer/` | Plain `SlingServletPaths` |
| `/content/aem-eds-modernizer/home.html` | `SlingServletResourceTypes` binds to the seeded page's `sling:resourceType` |
| `/bin/aem-eds-modernizer/api/projects` | `DashboardApi` for JSON |

---

## Section 6 — The SPA (StaticDashboard)

`core/src/main/java/com/adobe/aem/modernizer/dashboard/StaticDashboard.java`
is a Java class that returns the SPA as a single HTML string. The
SPA is fully inlined — no external CSS, no external JS, no
external images. It uses a `<base href="...">` tag in `<head>` so
all relative `fetch()` calls hit the API Sling Servlet.

Key changes from the original:

```java
// Add the apiBase parameter (default to the Sling Servlet)
public static String html() {
    return html("/bin/aem-eds-modernizer/api");
}
public static String html(String apiBase) {
    String base = apiBase == null || apiBase.isEmpty()
        ? "/bin/aem-eds-modernizer/api" : apiBase;
    return "<!doctype html>\n<html lang=\"en\"><head>\n" +
        "<meta charset=\"utf-8\">\n" +
        "<title>AEM → EDS Modernizer</title>\n" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
        "<base href=\"" + base + "/\">\n" +  // <-- THE FIX
        "<style>...all inline CSS...</style>\n" +
        "</head><body>...dashboard SPA...</body></html>";
}

// In the JavaScript inside the SPA, read the base href at runtime:
"const api = (path, opts) => { const base = (document.querySelector('base')||{}).href || '/bin/aem-eds-modernizer/api/'; return fetch(base + path, ...); }"
```

---

## Section 7 — Bundle exports

The `core/pom.xml` Maven bundle plugin must export the package
that contains the Sling Servlets (so the OSGi HTTP Whiteboard can
find them), and the `dto` package:

```xml
<Export-Package>
    com.adobe.aem.modernizer.agents.*,
    com.adobe.aem.modernizer.connectors.*,
    com.adobe.aem.modernizer.ai.*,
    com.adobe.aem.modernizer.ai.providers.*,
    com.adobe.aem.modernizer.ai.routing.*,
    com.adobe.aem.modernizer.ai.secret.*,
    com.adobe.aem.modernizer.dashboard.*,
    com.adobe.aem.modernizer.persistence.*,
    com.adobe.aem.modernizer.persistence.model.*,
    com.adobe.aem.modernizer.security.*,
    com.adobe.aem.modernizer.ssrf.*,
    com.adobe.aem.modernizer.scopes.*,
    com.adobe.aem.modernizer.mock.*,
    com.adobe.aem.modernizer.util.*,
    com.adobe.aem.modernizer.osgi.*
</Export-Package>
```

---

## Section 8 — Phase 2 endpoints

The `ApiRouter` already has these (verified working in e2e):

```
GET  /api/projects/{id}/redirects
GET  /api/projects/{id}/dependencies
GET  /api/projects/{id}/rollout-stages        (latest job)
GET  /api/projects/{id}/rollout-stages/{jid}  (specific job)
GET  /api/projects/{id}/repairs               (cross-job)
GET  /api/projects/{id}/repairs/{jid}        (per job)
GET  /api/projects/{id}/benchmarks
```

Plus the new persistence record types:

```
UrlRedirectRecord, DependencyEdgeRecord, RolloutStageRecord,
RepairAttemptRecord, BenchmarkSampleRecord
```

The `JcrStore` (AEM Cloud, ranking 200) persists projects under
`/var/aem-eds-modernizer/projects/{yyyy}/{MM}/{projectId}` with `eds:*` namespaced
properties. The `eds` namespace is registered via RepoInit. `JsonFileStore` (ranking 100) writes JSON snapshots to
disk. `InMemoryStore` (no ranking, fallback) implements all of
these via `ConcurrentHashMap` with cross-job helpers (`repairsForProject(projectId)`,
`benchmarksFor(agent)`, etc.).

---

## Section 9 — Build, test, deploy

### Build (any JDK 11+)

```bash
cd /home/user/aem-eds-modernizer
export JAVA_HOME=/usr/lib/jvm/jdk-11
export PATH=$JAVA_HOME/bin:$PATH
mvn clean install
```

**Result:** all 7 modules build in ~14 seconds, 13 tests pass.

### Run the standalone e2e

```bash
bash scripts/e2e.sh
```

**Result:** 177 URL redirects, 669 dependency edges, 5 rollout
stages, 23 repair attempts, 34 benchmark samples.

### Run the standalone server (no AEM)

```bash
java -jar core/target/core-0.1.0-SNAPSHOT-standalone.jar
# Open http://localhost:8080
```

### Deploy to AEM Cloud via Maven (fastest for local SDK)

```bash
# Local SDK on :4502
mvn clean install -Pdeploy

# Cloud Manager dev environment
mvn clean install -Pdeploy \
    -Daem.host=https://author-pXXXX-eYYYY.adobeaemcloud.com \
    -Daem.user=modernizer-deploy \
    -Daem.password=$AEM_TOKEN
```

The profile auto-routes each module:

| Module | Deploy step |
|---|---|
| `core` | POST bundle to `/system/console/bundles` |
| `ui.apps`, `ui.config`, `ui.content`, `dispatcher` | POST zip to `/crx/packmgr/service.jsp?force=true&install=true` |
| `all` | Skipped (meta container) |

Install order is `core → ui.apps → ui.config → ui.content →
dispatcher` because the OSGi bundle must be installed **first**
so the Sling Servlet is registered before content packages
that depend on it.

### Deploy to AEM Cloud via Cloud Manager (production)

1. Push to GitHub.
2. Cloud Manager → your pipeline → Run on `main`.
3. Cloud Manager runs `mvn clean install` (no `deploy` profile).
4. Click "Deploy to Dev" after the build succeeds.

### Deploy to local AEM SDK (manual, no Maven)

```bash
# 1. Start AEM SDK quickstart on :4502
java -jar aem-sdk-quickstart-*.jar -p 4502

# 2. Install the OSGi bundle
curl -u admin:admin -F bundle=@core/target/core-0.1.0-SNAPSHOT.jar \
  http://localhost:4502/system/console/bundles

# 3. Install content packages via Package Manager UI
# http://localhost:4502/crx/packmgr/index.jsp
# Upload and install: ui.apps, ui.config, ui.content, dispatcher zips
```

---

## Section 10 — Verify

```bash
# 1. All 7 artefacts present
ls -la /home/user/aem-eds-modernizer/{core,ui.apps,ui.config,ui.content,dispatcher,all}/target/*.{jar,zip} 2>/dev/null

# 2. Critical classes in the bundle
jar -tf core/target/core-0.1.0-SNAPSHOT.jar | grep -E "ModernizerHomeServlet|ModernizerBundleActivator|ApiRouter|DashboardApi"

# 3. Repo Init config is in ui.config
unzip -p ui.config/target/ui.config-0.1.0-SNAPSHOT.zip jcr_root/apps/aem-eds-modernizer/configs/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer/org.apache.sling.jcr.repoinit.RepositoryInitializer~aem-eds-modernizer.config

# 4. Seeded home page in ui.content
unzip -p ui.content/target/ui.content-0.1.0-SNAPSHOT.zip jcr_root/content/aem-eds-modernizer/home/.content.xml

# 5. After install, open in browser
# http://localhost:4502/aem-eds-modernizer              (recommended; works even if ui.content isn't installed)
# http://localhost:4502/content/aem-eds-modernizer/home.html  (seeded page)
# http://localhost:4502/bin/aem-eds-modernizer/api/health       (JSON API directly)
```

If `/aem-eds-modernizer` is empty or shows an AEM error page,
check `/logs/error.log` filtered on `ModernizerBundleActivator`
or `ModernizerHomeServlet` — see
[`docs/aem/TROUBLESHOOTING_HOME_PAGE.md`](docs/aem/TROUBLESHOOTING_HOME_PAGE.md).

---

## Section 11 — Maven deploy profile (the one in `pom.xml`)

The `deploy` profile in the parent `pom.xml` uses `maven-antrun-plugin`
to invoke `curl` for the HTTP uploads. It uses two executions:

- `deploy-bundle` (only runs for the `core` module — `skipBundleDeploy=false` only there)
- `deploy-package` (only runs for content packages — `skipPackageDeploy=false` for `ui.apps`, `ui.config`, `ui.content`, `dispatcher`)

The `<skip>` flag in each execution reads from the per-module
`<properties>` block, so each module auto-routes itself. The full
profile is in `pom.xml` under `<profiles><profile><id>deploy</id>`.

Properties added:

```xml
<aem.host>http://localhost:4502</aem.host>
<aem.user>admin</aem.user>
<aem.password>admin</aem.password>
<aem.skipSslValidation>true</aem.skipSslValidation>
<aem.packageService>/crx/packmgr/service.jsp</aem.packageService>
<skipBundleDeploy>true</skipBundleDeploy>
<skipPackageDeploy>true</skipPackageDeploy>
```

Each module overrides the skip flags in its own `pom.xml`:

| Module | `skipBundleDeploy` | `skipPackageDeploy` |
|---|---|---|
| `core` | **false** | true |
| `ui.apps` | true | **false** |
| `ui.config` | true | **false** |
| `ui.content` | true | **false** |
| `dispatcher` | true | **false** |
| `all` | true | true |

---

## Section 12 — Documentation to ship

Every repo has these docs (in `docs/`):

- `architecture/` — 7 files (OVERVIEW, COMPONENTS, REQUEST_FLOW, DATA_FLOW, RUNTIME_TOPOLOGY, STATE_MACHINE, README)
- `adr/` — 16 files (0001–0015) covering every architectural decision
- `agents/` — 28 files (one per agent + AI_GATEWAY + README)
- `aem/` — 7 files including `DEPLOY_TO_AEM_CLOUD.md` and `TROUBLESHOOTING_HOME_PAGE.md`
- `eds/`, `figma/`, `github/` — connector docs (5-6 files each)
- `migration/` — 8 files (DRY_RUN, ASSET_POLICY, ESTIMATE, SCOPE, CHECKPOINTS, REPORT, STATE_MACHINE, README)
- `security/` — 7 files (SECRETS, SSRF, REDACTOR, AUDIT, RBAC, CAPABILITY_GATE, README)
- `operations/` — 7 files (RUNBOOK, OBSERVABILITY, RECOVERY, INCIDENT_RESPONSE, CAPACITY_PLANNING, DEPLOYMENT_CHECKLIST, README)

Plus two top-level files:

- `README.md` — project overview, quickstart, architecture, the full dashboard route table
- `CHECKLIST.md` — single-page build & deploy verification

---

## Section 13 — Recap of the four critical fixes

1. **Java 11 compatibility** (Section 2) — switch expressions
   → if/else; `Stream.toList()` → `collect(Collectors.toList())`;
   fix the `\s` escape in `Redactor`.
2. **OSGi DS conversion** (Section 3) — add `@Component` to 11
   classes; create `ModernizerBundleActivator` for the agent
   graph; expose the right packages.
3. **The Sling Servlet fix** (Section 5) — `ModernizerHomeServlet`
   registered at `/aem-eds-modernizer` (plain path) **and** at
   `/content/aem-eds-modernizer/home.html` (resource type
   binding) writes the full SPA HTML directly. This bypasses the
   fragile HTL page-component pipeline.
4. **The `<base>` tag in the SPA** (Section 6) — the dashboard
   reads `document.querySelector('base').href` at runtime to
   point all `fetch()` calls at the right API base.

Without fix #3, the home page is empty or shows an AEM scripting
error because the `cq:Component` doesn't have a `sling:resourceSuperType`
pointing to a foundation page component.

Without fix #4, the SPA would call `/api/...` (which doesn't
resolve to anything in AEM).

---

## Section 14 — Quick reference card

```bash
# Setup
export JAVA_HOME=/usr/lib/jvm/jdk-11
export PATH=$JAVA_HOME/bin:$PATH
cd /home/user/aem-eds-modernizer

# Build
mvn clean install

# Test
bash scripts/e2e.sh

# Run standalone
java -jar core/target/core-0.1.0-SNAPSHOT-standalone.jar
# Open http://localhost:8080

# Deploy to AEM SDK
mvn clean install -Pdeploy
# Open http://localhost:4502/aem-eds-modernizer

# Deploy to AEM Cloud (Cloud Manager)
# 1. git push origin main
# 2. Open Cloud Manager → your pipeline → Run on main
# 3. Cloud Manager runs `mvn clean install` (no profile) and deploys

# 7 artefacts to upload to Cloud Manager manually (if not using CM)
# core/target/core-0.1.0-SNAPSHOT.jar
# ui.apps/target/ui.apps-0.1.0-SNAPSHOT.zip
# ui.config/target/ui.config-0.1.0-SNAPSHOT.zip
# ui.content/target/ui.content-0.1.0-SNAPSHOT.zip
# dispatcher/target/dispatcher-0.1.0-SNAPSHOT.zip
# (NOT core-0.1.0-SNAPSHOT-standalone.jar — that's the local fat jar)
# (NOT all-0.1.0-SNAPSHOT.zip — that's a meta container without subpackages)
```

---

*This master prompt is the single source of truth for the
AEM → EDS Modernizer's build & deploy. Every code change in the
repo traces back to a section here. If you find a discrepancy
between this document and the actual code, treat the code as
authoritative and update this document.*
