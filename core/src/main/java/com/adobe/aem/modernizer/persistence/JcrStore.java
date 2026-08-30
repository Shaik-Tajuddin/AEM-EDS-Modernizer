package com.adobe.aem.modernizer.persistence;

import com.adobe.aem.modernizer.persistence.model.ProjectRecord;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.jackrabbit.util.Text;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JCR-backed Store (crx/de visible): every mutation is persisted under
 * {@code /var/aem-eds-modernizer/projects/{yyyy}/{MM}/{escapedProjectId}}
 * as an nt:unstructured node so saved projects are visible and durable in
 * the AEM repository itself.
 *
 * <p>The {@code eds} namespace is registered via Repo Init (see ui.config),
 * not programmatically. The service user holds least privilege on
 * {@code /var/aem-eds-modernizer} only.
 *
 * <p>Inherits all in-memory behavior from {@link InMemoryStore}; on activation
 * the projects are reloaded from JCR so state survives bundle restarts.
 */
@Component(service = Store.class, immediate = true, property = { "service.ranking:Integer=200" })
public class JcrStore extends InMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(JcrStore.class);
    static final String ROOT_PATH = "/var/aem-eds-modernizer/projects";
    private static final String PROP_PREFIX = "eds:";

    private static final DateTimeFormatter SHARD_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

    @Reference
    private transient SlingRepository repository;

    private transient boolean jcrAvailable;

    @Activate
    public void activate() {
        jcrAvailable = repository != null;
        if (jcrAvailable) {
            loadProjectsFromJcr();
            LOG.info("JcrStore activated — projects persisted under {}", ROOT_PATH);
        } else {
            LOG.warn("JcrStore activated without SlingRepository — falling back to in-memory only");
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

    // ── Path helpers ──

    /**
     * Returns the shard parent path: {@code /var/aem-eds-modernizer/projects/{yyyy}/{MM}}.
     */
    static String shardPath(long epochMs) {
        return ROOT_PATH + "/" + SHARD_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    /**
     * Escapes a project id into a valid JCR node name using Jackrabbit's
     * {@link Text#escapeIllegalJcrChars(String)}. The original id is stored
     * as the {@code eds:projectId} property so it can always be recovered.
     */
    static String escapeNodeName(String id) {
        return Text.escapeIllegalJcrChars(id);
    }

    // ── JCR persistence ──

    private Session login() throws RepositoryException {
        try {
            return repository.loginService("modernizer-service", null);
        } catch (Exception e) {
            // Fallback for local AEM SDK where service-user mapping may not be configured.
            // In AEMaaCS, loginService always succeeds; this branch is local-dev only.
            LOG.debug("loginService unavailable ({}), falling back to admin login", e.getMessage());
            return repository.login(new javax.jcr.SimpleCredentials("admin", "admin".toCharArray()));
        }
    }

    private void persistProjectToJcr(ProjectRecord project) {
        Session session = null;
        try {
            session = login();
            Node shard = ensureShard(session, project.getCreatedAt());
            String nodeName = escapeNodeName(project.getId());
            Node node = shard.hasNode(nodeName) ? shard.getNode(nodeName) : shard.addNode(nodeName, "nt:unstructured");
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
            String nodePath = findProjectPath(session, id);
            if (nodePath != null) {
                session.removeItem(nodePath);
                session.save();
                LOG.info("Deleted project node {}", nodePath);
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
            int loaded = 0;
            loaded += loadFromTree(session, root);
            if (loaded > 0) LOG.info("Restored {} project(s) from JCR {}", loaded, ROOT_PATH);
        } catch (Exception e) {
            LOG.error("Failed to load projects from JCR: {}", e.getMessage(), e);
        } finally {
            if (session != null) session.logout();
        }
    }

    /**
     * Recursively walk the year/month shard tree to find project nodes.
     */
    private int loadFromTree(Session session, Node parent) throws RepositoryException {
        int loaded = 0;
        NodeIterator it = parent.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            if (child.hasProperty(PROP_PREFIX + "projectId")) {
                try {
                    ProjectRecord p = nodeToRecord(child);
                    super.saveProject(p);
                    loaded++;
                } catch (Exception perNode) {
                    LOG.warn("Skipping unparsable project node {}: {}", child.getPath(), perNode.getMessage());
                }
            } else {
                loaded += loadFromTree(session, child);
            }
        }
        return loaded;
    }

    private ProjectRecord nodeToRecord(Node node) throws RepositoryException {
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
        return p;
    }

    /**
     * Find a project node by id by scanning the shard tree.
     * Returns the absolute path or null.
     */
    private String findProjectPath(Session session, String projectId) throws RepositoryException {
        if (!session.nodeExists(ROOT_PATH)) return null;
        return findInTree(session.getNode(ROOT_PATH), projectId);
    }

    private String findInTree(Node parent, String projectId) throws RepositoryException {
        NodeIterator it = parent.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            if (child.hasProperty(PROP_PREFIX + "projectId")) {
                if (projectId.equals(child.getProperty(PROP_PREFIX + "projectId").getString())) {
                    return child.getPath();
                }
            } else {
                String found = findInTree(child, projectId);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Node ensureShard(Session session, long epochMs) throws RepositoryException {
        String shardRel = SHARD_FMT.format(Instant.ofEpochMilli(epochMs));
        String shardPath = ROOT_PATH + "/" + shardRel;
        if (session.nodeExists(shardPath)) return session.getNode(shardPath);

        ensureRoot(session);
        Node node = session.getNode(ROOT_PATH);
        for (String segment : shardRel.split("/")) {
            node = node.hasNode(segment) ? node.getNode(segment) : node.addNode(segment, "sling:Folder");
        }
        return node;
    }

    private void ensureRoot(Session session) throws RepositoryException {
        if (session.nodeExists(ROOT_PATH)) return;
        Node varNode = session.getRootNode().hasNode("var")
                ? session.getRootNode().getNode("var")
                : session.getRootNode().addNode("var", "sling:Folder");
        Node modernizer = varNode.hasNode("aem-eds-modernizer")
                ? varNode.getNode("aem-eds-modernizer")
                : varNode.addNode("aem-eds-modernizer", "sling:Folder");
        if (!modernizer.hasNode("projects")) {
            modernizer.addNode("projects", "sling:Folder");
        }
    }

    private void setString(Node node, ProjectRecord p, String field) throws RepositoryException {
        try {
            String value = (String) ProjectRecord.class.getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1)).invoke(p);
            if (value != null && !value.isEmpty()) {
                node.setProperty(PROP_PREFIX + field, value);
            }
        } catch (Exception ignored) {
        }
    }

    private String getString(Node node, String field) throws RepositoryException {
        return node.hasProperty(PROP_PREFIX + field) ? node.getProperty(PROP_PREFIX + field).getString() : null;
    }

    private String orDefault(String v, String dflt) {
        return (v == null || v.isEmpty()) ? dflt : v;
    }
}
