package com.adobe.aem.modernizer.rag.persistence;

import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncRun;
import com.adobe.aem.modernizer.util.JsonUtil;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JCR-backed RAG store persisting knowledge documents, chunks, and sync runs under
 * {@code /var/modernizer/rag/projects/{projectId}/...} with 2-character prefix sharding
 * to guarantee high performance and avoid Oak child-node scaling bottlenecks.
 */
@Component(service = {RagStore.class, JcrRagStore.class}, immediate = true, property = {"service.ranking:Integer=200"})
public class JcrRagStore implements RagStore {

    private static final Logger LOG = LoggerFactory.getLogger(JcrRagStore.class);
    public static final String ROOT_PATH = "/var/modernizer/rag";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient SlingRepository repository;

    private final Map<String, KnowledgeDocument> documentCache = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeChunk> chunkCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> projectDocumentIds = new ConcurrentHashMap<>();
    private final Map<String, List<String>> projectChunkIds = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeSyncRun> syncRuns = new ConcurrentHashMap<>();

    private volatile boolean jcrAvailable = false;

    public JcrRagStore() {
    }

    public JcrRagStore(SlingRepository repository) {
        this.repository = repository;
        this.jcrAvailable = (repository != null);
    }

    @Activate
    public void activate() {
        this.jcrAvailable = (repository != null);
        if (jcrAvailable) {
            LOG.info("JcrRagStore activated with SlingRepository — root: {}", ROOT_PATH);
            loadStateFromJcr();
        } else {
            LOG.warn("JcrRagStore running in in-memory mode without JCR repository.");
        }
    }

    @Deactivate
    public void deactivate() {
        LOG.info("JcrRagStore deactivated");
    }

    // ── Document Operations ──

    @Override
    public void saveDocument(KnowledgeDocument doc) {
        if (doc == null || doc.getId() == null) return;
        documentCache.put(doc.getId(), doc);

        String projectId = (doc.getMetadata() != null && doc.getMetadata().getProjectId() != null)
                ? doc.getMetadata().getProjectId() : "default";
        projectDocumentIds.computeIfAbsent(projectId, k -> Collections.synchronizedList(new ArrayList<>())).add(doc.getId());

        if (jcrAvailable) {
            persistDocumentToJcr(projectId, doc);
        }
    }

    @Override
    public Optional<KnowledgeDocument> getDocument(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(documentCache.get(id));
    }

    @Override
    public List<KnowledgeDocument> listDocuments(String projectId) {
        String pid = (projectId != null && !projectId.isBlank()) ? projectId : "default";
        List<String> docIds = projectDocumentIds.get(pid);
        if (docIds == null || docIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeDocument> result = new ArrayList<>();
        synchronized (docIds) {
            for (String docId : docIds) {
                KnowledgeDocument d = documentCache.get(docId);
                if (d != null) {
                    result.add(d);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deleteDocument(String id) {
        if (id == null) return false;
        KnowledgeDocument removed = documentCache.remove(id);
        if (removed != null) {
            String projectId = (removed.getMetadata() != null && removed.getMetadata().getProjectId() != null)
                    ? removed.getMetadata().getProjectId() : "default";
            List<String> docIds = projectDocumentIds.get(projectId);
            if (docIds != null) {
                docIds.remove(id);
            }
            if (jcrAvailable) {
                deleteDocumentFromJcr(projectId, id);
            }
            return true;
        }
        return false;
    }

    // ── Chunk Operations ──

    @Override
    public void saveChunk(KnowledgeChunk chunk) {
        if (chunk == null || chunk.getChunkId() == null) return;
        chunkCache.put(chunk.getChunkId(), chunk);

        String projectId = (chunk.getMetadata() != null && chunk.getMetadata().getProjectId() != null)
                ? chunk.getMetadata().getProjectId() : "default";
        projectChunkIds.computeIfAbsent(projectId, k -> Collections.synchronizedList(new ArrayList<>())).add(chunk.getChunkId());

        if (jcrAvailable) {
            persistChunkToJcr(projectId, chunk);
        }
    }

    @Override
    public void saveChunks(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;
        for (KnowledgeChunk c : chunks) {
            saveChunk(c);
        }
    }

    @Override
    public Optional<KnowledgeChunk> getChunk(String chunkId) {
        if (chunkId == null) return Optional.empty();
        return Optional.ofNullable(chunkCache.get(chunkId));
    }

    @Override
    public List<KnowledgeChunk> listChunksForDocument(String documentId) {
        if (documentId == null) return Collections.emptyList();
        List<KnowledgeChunk> matched = new ArrayList<>();
        for (KnowledgeChunk chunk : chunkCache.values()) {
            if (documentId.equals(chunk.getDocumentId())) {
                matched.add(chunk);
            }
        }
        return matched;
    }

    @Override
    public List<KnowledgeChunk> listChunksForProject(String projectId) {
        String pid = (projectId != null && !projectId.isBlank()) ? projectId : "default";
        List<String> chunkIds = projectChunkIds.get(pid);
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeChunk> result = new ArrayList<>();
        synchronized (chunkIds) {
            for (String cid : chunkIds) {
                KnowledgeChunk c = chunkCache.get(cid);
                if (c != null) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    @Override
    public boolean deleteChunk(String chunkId) {
        if (chunkId == null) return false;
        KnowledgeChunk removed = chunkCache.remove(chunkId);
        if (removed != null) {
            String projectId = (removed.getMetadata() != null && removed.getMetadata().getProjectId() != null)
                    ? removed.getMetadata().getProjectId() : "default";
            List<String> list = projectChunkIds.get(projectId);
            if (list != null) {
                list.remove(chunkId);
            }
            if (jcrAvailable) {
                deleteChunkFromJcr(projectId, chunkId);
            }
            return true;
        }
        return false;
    }

    // ── Sync Runs ──

    @Override
    public void saveSyncRun(KnowledgeSyncRun run) {
        if (run == null || run.getSyncId() == null) return;
        syncRuns.put(run.getSyncId(), run);
        if (jcrAvailable) {
            persistSyncRunToJcr(run);
        }
    }

    @Override
    public Optional<KnowledgeSyncRun> getSyncRun(String syncId) {
        if (syncId == null) return Optional.empty();
        return Optional.ofNullable(syncRuns.get(syncId));
    }

    @Override
    public List<KnowledgeSyncRun> listSyncRuns(String projectId) {
        List<KnowledgeSyncRun> list = new ArrayList<>();
        for (KnowledgeSyncRun r : syncRuns.values()) {
            if (projectId == null || projectId.equalsIgnoreCase(r.getProjectId())) {
                list.add(r);
            }
        }
        list.sort((a, b) -> String.valueOf(b.getStartTime()).compareTo(String.valueOf(a.getStartTime())));
        return list;
    }

    @Override
    public Optional<KnowledgeSyncRun> getLatestSyncRun(String projectId) {
        List<KnowledgeSyncRun> list = listSyncRuns(projectId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public long getDocumentCount(String projectId) {
        return listDocuments(projectId).size();
    }

    @Override
    public long getChunkCount(String projectId) {
        return listChunksForProject(projectId).size();
    }

    // ── JCR Helpers ──

    private Session login() throws RepositoryException {
        try {
            return repository.loginService("modernizer-service", null);
        } catch (Exception e) {
            return repository.login(new javax.jcr.SimpleCredentials("admin", "admin".toCharArray()));
        }
    }

    private Node ensurePath(Session session, String path) throws RepositoryException {
        String[] parts = path.split("/");
        Node current = session.getRootNode();
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            if (!current.hasNode(part)) {
                current = current.addNode(part, "nt:unstructured");
            } else {
                current = current.getNode(part);
            }
        }
        return current;
    }

    private void persistDocumentToJcr(String projectId, KnowledgeDocument doc) {
        Session session = null;
        try {
            session = login();
            String docPath = ROOT_PATH + "/projects/" + Text.escapeIllegalJcrChars(projectId) + "/documents";
            Node parent = ensurePath(session, docPath);
            String nodeName = Text.escapeIllegalJcrChars(doc.getId());
            Node node = parent.hasNode(nodeName) ? parent.getNode(nodeName) : parent.addNode(nodeName, "nt:unstructured");

            node.setProperty("docId", doc.getId());
            node.setProperty("sourceId", doc.getSourceId() != null ? doc.getSourceId() : "");
            node.setProperty("path", doc.getPath() != null ? doc.getPath() : "");
            node.setProperty("title", doc.getTitle() != null ? doc.getTitle() : "");
            node.setProperty("documentType", doc.getDocumentType() != null ? doc.getDocumentType() : "");
            node.setProperty("fingerprint", doc.getFingerprint() != null ? doc.getFingerprint() : "");
            node.setProperty("status", doc.getStatus() != null ? doc.getStatus() : "");
            node.setProperty("payloadJson", JsonUtil.toJson(doc));

            session.save();
        } catch (Exception e) {
            LOG.warn("Failed persisting KnowledgeDocument to JCR: {}", doc.getId(), e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }

    private void deleteDocumentFromJcr(String projectId, String docId) {
        Session session = null;
        try {
            session = login();
            String nodePath = ROOT_PATH + "/projects/" + Text.escapeIllegalJcrChars(projectId) + "/documents/" + Text.escapeIllegalJcrChars(docId);
            if (session.nodeExists(nodePath)) {
                session.getNode(nodePath).remove();
                session.save();
            }
        } catch (Exception e) {
            LOG.warn("Failed removing document from JCR: {}", docId, e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }

    private void persistChunkToJcr(String projectId, KnowledgeChunk chunk) {
        Session session = null;
        try {
            session = login();
            // 2-character prefix sharding
            String cid = chunk.getChunkId();
            String prefix = (cid != null && cid.length() >= 6) ? cid.substring(4, 6) : "00";
            String chunkParentPath = ROOT_PATH + "/projects/" + Text.escapeIllegalJcrChars(projectId) + "/chunks/" + prefix;
            Node parent = ensurePath(session, chunkParentPath);
            String nodeName = Text.escapeIllegalJcrChars(cid);
            Node node = parent.hasNode(nodeName) ? parent.getNode(nodeName) : parent.addNode(nodeName, "nt:unstructured");

            node.setProperty("chunkId", chunk.getChunkId());
            node.setProperty("documentId", chunk.getDocumentId() != null ? chunk.getDocumentId() : "");
            node.setProperty("path", chunk.getPath() != null ? chunk.getPath() : "");
            node.setProperty("heading", chunk.getHeading() != null ? chunk.getHeading() : "");
            node.setProperty("section", chunk.getSection() != null ? chunk.getSection() : "");
            node.setProperty("chunkType", chunk.getChunkType() != null ? chunk.getChunkType() : "");
            node.setProperty("fingerprint", chunk.getFingerprint() != null ? chunk.getFingerprint() : "");
            node.setProperty("tokenCount", (long) chunk.getTokenCount());
            node.setProperty("payloadJson", JsonUtil.toJson(chunk));

            session.save();
        } catch (Exception e) {
            LOG.warn("Failed persisting KnowledgeChunk to JCR: {}", chunk.getChunkId(), e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }

    private void deleteChunkFromJcr(String projectId, String chunkId) {
        Session session = null;
        try {
            session = login();
            String prefix = (chunkId != null && chunkId.length() >= 6) ? chunkId.substring(4, 6) : "00";
            String nodePath = ROOT_PATH + "/projects/" + Text.escapeIllegalJcrChars(projectId) + "/chunks/" + prefix + "/" + Text.escapeIllegalJcrChars(chunkId);
            if (session.nodeExists(nodePath)) {
                session.getNode(nodePath).remove();
                session.save();
            }
        } catch (Exception e) {
            LOG.warn("Failed removing chunk from JCR: {}", chunkId, e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }

    private void persistSyncRunToJcr(KnowledgeSyncRun run) {
        Session session = null;
        try {
            session = login();
            String path = ROOT_PATH + "/projects/" + Text.escapeIllegalJcrChars(run.getProjectId() != null ? run.getProjectId() : "default") + "/sync-runs";
            Node parent = ensurePath(session, path);
            String nodeName = Text.escapeIllegalJcrChars(run.getSyncId());
            Node node = parent.hasNode(nodeName) ? parent.getNode(nodeName) : parent.addNode(nodeName, "nt:unstructured");

            node.setProperty("syncId", run.getSyncId());
            node.setProperty("status", run.getStatus() != null ? run.getStatus() : "");
            node.setProperty("processed", (long) run.getDocumentsProcessed());
            node.setProperty("chunksCreated", (long) run.getChunksCreated());
            node.setProperty("payloadJson", JsonUtil.toJson(run));

            session.save();
        } catch (Exception e) {
            LOG.warn("Failed persisting KnowledgeSyncRun to JCR: {}", run.getSyncId(), e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }

    private void loadStateFromJcr() {
        Session session = null;
        try {
            session = login();
            if (!session.nodeExists(ROOT_PATH + "/projects")) return;
            Node projectsNode = session.getNode(ROOT_PATH + "/projects");
            NodeIterator projectIter = projectsNode.getNodes();

            while (projectIter.hasNext()) {
                Node projNode = projectIter.nextNode();
                String projectId = projNode.getName();

                // Load Documents
                if (projNode.hasNode("documents")) {
                    Node docsNode = projNode.getNode("documents");
                    NodeIterator docIter = docsNode.getNodes();
                    while (docIter.hasNext()) {
                        Node dNode = docIter.nextNode();
                        if (dNode.hasProperty("payloadJson")) {
                            String json = dNode.getProperty("payloadJson").getString();
                            KnowledgeDocument doc = JsonUtil.fromJson(json, KnowledgeDocument.class);
                            if (doc != null) {
                                documentCache.put(doc.getId(), doc);
                                projectDocumentIds.computeIfAbsent(projectId, k -> Collections.synchronizedList(new ArrayList<>())).add(doc.getId());
                            }
                        }
                    }
                }

                // Load Chunks (traverse shard folders)
                if (projNode.hasNode("chunks")) {
                    Node chunksRoot = projNode.getNode("chunks");
                    NodeIterator shardIter = chunksRoot.getNodes();
                    while (shardIter.hasNext()) {
                        Node shard = shardIter.nextNode();
                        NodeIterator chunkIter = shard.getNodes();
                        while (chunkIter.hasNext()) {
                            Node cNode = chunkIter.nextNode();
                            if (cNode.hasProperty("payloadJson")) {
                                String json = cNode.getProperty("payloadJson").getString();
                                KnowledgeChunk chunk = JsonUtil.fromJson(json, KnowledgeChunk.class);
                                if (chunk != null) {
                                    chunkCache.put(chunk.getChunkId(), chunk);
                                    projectChunkIds.computeIfAbsent(projectId, k -> Collections.synchronizedList(new ArrayList<>())).add(chunk.getChunkId());
                                }
                            }
                        }
                    }
                }
            }
            LOG.info("Loaded {} documents and {} chunks from JCR into cache", documentCache.size(), chunkCache.size());
        } catch (Exception e) {
            LOG.warn("Unable to preload RAG state from JCR", e);
        } finally {
            if (session != null && session.isLive()) session.logout();
        }
    }
}
