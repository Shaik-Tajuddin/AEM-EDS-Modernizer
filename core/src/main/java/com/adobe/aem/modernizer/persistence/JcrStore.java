package com.adobe.aem.modernizer.persistence;

import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;

/**
 * JCR-backed Store (crx/de visible): every mutation is persisted under
 * {@code /conf/aem-eds-modernizer/<Project ID>} as an nt:unstructured node so
 * saved projects are visible and durable in the AEM repository itself.
 *
 * Inherits all in-memory behavior from {@link InMemoryStore}; on activation the
 * projects are reloaded from JCR so state survives bundle restarts.
 */
@Component(service = Store.class, immediate = true, property = { "service.ranking:Integer=200" })
public class JcrStore extends InMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(JcrStore.class);
    static final String ROOT_PATH = "/conf/aem-eds-modernizer";
    private static final String PROP_PREFIX = "eds:";
    private static final String NS_PREFIX = "eds";
    private static final String NS_URI = "https://www.adobe.com/aem-eds-modernizer/1.0";

    @Reference
    private transient SlingRepository repository;

    private transient boolean jcrAvailable;

    @Activate
    public void activate() {
        jcrAvailable = repository != null;
        if (jcrAvailable) {
            registerNamespace();
            loadProjectsFromJcr();
            LOG.info("JcrStore activated — projects persisted under {}", ROOT_PATH);
        } else {
            LOG.warn("JcrStore activated without SlingRepository — falling back to in-memory only");
        }
    }

    private void registerNamespace() {
        // Namespace registration requires repository-wide jcr:namespaceManagement,
        // which the scoped service user is not granted — use admin explicitly.
        Session session = null;
        try {
            session = repository.login(new javax.jcr.SimpleCredentials("admin", "admin".toCharArray()));
            javax.jcr.NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            for (String prefix : registry.getPrefixes()) {
                if (NS_PREFIX.equals(prefix)) {
                    return;
                }
            }
            registry.registerNamespace(NS_PREFIX, NS_URI);
            LOG.info("Registered JCR namespace '{}' -> {}", NS_PREFIX, NS_URI);
        } catch (Exception e) {
            LOG.error("Failed to register '{}' namespace: {}", NS_PREFIX, e.getMessage(), e);
        } finally {
            if (session != null) session.logout();
        }
    }

    @Deactivate
    public void deactivate() {
        LOG.info("JcrStore deactivated");
    }

    // ── Overrides: every project mutation persists to JCR ──

    @Override
    public void saveProject(ProjectRecord project) {
        super.saveProject(project);
        if (jcrAvailable && project != null && project.getId() != null) {
            persistProjectToJcr(project);
        }
    }

    @Override
    public void deleteProject(String id) {
        super.deleteProject(id);
        if (jcrAvailable && id != null) {
            deleteProjectFromJcr(id);
        }
    }

    // ── JCR persistence ──

    private Session login() throws RepositoryException {
        // Prefer the dedicated service user; fall back to admin login (local SDK / author)
        try {
            return repository.loginService(null, null);
        } catch (Exception e) {
            LOG.debug("loginService unavailable ({}), falling back to admin login", e.getMessage());
            return repository.login(new javax.jcr.SimpleCredentials("admin", "admin".toCharArray()));
        }
    }

    private void persistProjectToJcr(ProjectRecord project) {
        Session session = null;
        try {
            session = login();
            Node root = ensureRoot(session);
            // Node name must be JCR-safe; sanitize the project id
            String nodeName = sanitize(project.getId());
            Node node = root.hasNode(nodeName) ? root.getNode(nodeName) : root.addNode(nodeName, "nt:unstructured");
            node.setProperty("jcr:title", project.getName() != null ? project.getName() : project.getId());
            node.setProperty(PROP_PREFIX + "projectId", project.getId());
            setString(node, project, "name");
            setString(node, project, "aemAuthorUrl");
            setString(node, project, "aemPublishUrl");
            setString(node, project, "contentRoot");
            setString(node, project, "pageScope");
            setString(node, project, "edsGitRepoUrl");
            setString(node, project, "edsBranch");
            setString(node, project, "figmaUrl");
            setString(node, project, "markerProperty");
            setString(node, project, "markerValue");
            setString(node, project, "authoringStrategy");
            setString(node, project, "aiProvider");
            setString(node, project, "aiModel");
            node.setProperty(PROP_PREFIX + "maxBudgetUsd", project.getMaxBudgetUsd());
            node.setProperty(PROP_PREFIX + "maxRepairAttempts", (long) project.getMaxRepairAttempts());
            node.setProperty(PROP_PREFIX + "createdAt", project.getCreatedAt());
            node.setProperty(PROP_PREFIX + "updatedAt", project.getUpdatedAt());
            if (project.getProperties() != null && !project.getProperties().isEmpty()) {
                node.setProperty(PROP_PREFIX + "properties", JsonUtil.toJson(project.getProperties()));
            }
            session.save();
            LOG.debug("Persisted project '{}' to {}", project.getId(), node.getPath());
        } catch (Exception e) {
            LOG.error("Failed to persist project '{}' to JCR: {}", project != null ? project.getId() : "?", e.getMessage(), e);
        } finally {
            if (session != null) session.logout();
        }
    }

    private void deleteProjectFromJcr(String id) {
        Session session = null;
        try {
            session = login();
            String path = ROOT_PATH + "/" + sanitize(id);
            if (session.nodeExists(path)) {
                session.removeItem(path);
                session.save();
                LOG.info("Deleted project node {}", path);
            }
        } catch (Exception e) {
            LOG.error("Failed to delete project '{}' from JCR: {}", id, e.getMessage(), e);
        } finally {
            if (session != null) session.logout();
        }
    }

    private void loadProjectsFromJcr() {
        Session session = null;
        try {
            session = login();
            if (!session.nodeExists(ROOT_PATH)) return;
            Node root = session.getNode(ROOT_PATH);
            NodeIterator it = root.getNodes();
            int loaded = 0;
            while (it.hasNext()) {
                Node node = it.nextNode();
                try {
                    if (!"nt:unstructured".equals(node.getPrimaryNodeType().getName())) continue;
                    if (!node.hasProperty(PROP_PREFIX + "projectId")) continue;
                    ProjectRecord p = new ProjectRecord();
                    p.setId(node.getProperty(PROP_PREFIX + "projectId").getString());
                    p.setName(getString(node, "name"));
                    p.setAemAuthorUrl(getString(node, "aemAuthorUrl"));
                    p.setAemPublishUrl(getString(node, "aemPublishUrl"));
                    p.setContentRoot(getString(node, "contentRoot"));
                    p.setPageScope(getString(node, "pageScope"));
                    p.setEdsGitRepoUrl(getString(node, "edsGitRepoUrl"));
                    p.setEdsBranch(getString(node, "edsBranch"));
                    p.setFigmaUrl(getString(node, "figmaUrl"));
                    p.setMarkerProperty(getString(node, "markerProperty"));
                    p.setMarkerValue(getString(node, "markerValue"));
                    p.setAuthoringStrategy(orDefault(getString(node, "authoringStrategy"), "UNIVERSAL_EDITOR"));
                    p.setAiProvider(getString(node, "aiProvider"));
                    p.setAiModel(getString(node, "aiModel"));
                    p.setMaxBudgetUsd(node.hasProperty(PROP_PREFIX + "maxBudgetUsd")
                            ? node.getProperty(PROP_PREFIX + "maxBudgetUsd").getDouble() : 100.0);
                    p.setMaxRepairAttempts(node.hasProperty(PROP_PREFIX + "maxRepairAttempts")
                            ? (int) node.getProperty(PROP_PREFIX + "maxRepairAttempts").getLong() : 5);
                    p.setCreatedAt(node.hasProperty(PROP_PREFIX + "createdAt")
                            ? node.getProperty(PROP_PREFIX + "createdAt").getLong() : System.currentTimeMillis());
                    p.setUpdatedAt(node.hasProperty(PROP_PREFIX + "updatedAt")
                            ? node.getProperty(PROP_PREFIX + "updatedAt").getLong() : System.currentTimeMillis());
                    if (node.hasProperty(PROP_PREFIX + "properties")) {
                        Map<String, Object> props = JsonUtil.fromJson(
                                node.getProperty(PROP_PREFIX + "properties").getString(), Map.class);
                        if (props != null) p.setProperties(props);
                    }
                    super.saveProject(p);
                    loaded++;
                } catch (Exception perNode) {
                    LOG.warn("Skipping unparsable project node {}: {}", node.getPath(), perNode.getMessage());
                }
            }
            if (loaded > 0) LOG.info("Restored {} project(s) from JCR {}", loaded, ROOT_PATH);
        } catch (Exception e) {
            LOG.error("Failed to load projects from JCR: {}", e.getMessage(), e);
        } finally {
            if (session != null) session.logout();
        }
    }

    private Node ensureRoot(Session session) throws RepositoryException {
        if (session.nodeExists(ROOT_PATH)) return session.getNode(ROOT_PATH);
        // /conf already exists in AEM — only create missing levels
        Node rootNode = session.getRootNode();
        Node conf = rootNode.hasNode("conf") ? rootNode.getNode("conf") : rootNode.addNode("conf", "sling:Folder");
        return conf.hasNode("aem-eds-modernizer")
                ? conf.getNode("aem-eds-modernizer")
                : conf.addNode("aem-eds-modernizer", "sling:Folder");
    }

    private void setString(Node node, ProjectRecord p, String field) throws RepositoryException {
        try {
            String value = (String) ProjectRecord.class.getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1)).invoke(p);
            if (value != null && !value.isEmpty()) {
                node.setProperty(PROP_PREFIX + field, value);
            }
        } catch (Exception ignored) {
            // reflection miss — field simply not persisted
        }
    }

    private String getString(Node node, String field) throws RepositoryException {
        return node.hasProperty(PROP_PREFIX + field) ? node.getProperty(PROP_PREFIX + field).getString() : null;
    }

    private String orDefault(String v, String dflt) {
        return (v == null || v.isEmpty()) ? dflt : v;
    }

    private String sanitize(String id) {
        return id.replaceAll("[^A-Za-z0-9-_ ]", "_").trim();
    }
}
