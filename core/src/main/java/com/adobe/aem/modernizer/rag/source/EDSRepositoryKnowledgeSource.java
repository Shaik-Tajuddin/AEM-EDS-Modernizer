package com.adobe.aem.modernizer.rag.source;

import com.adobe.aem.modernizer.connectors.GitHubClient;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncContext;
import com.adobe.aem.modernizer.rag.util.FingerprintUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Production-ready KnowledgeSource scanning the Edge Delivery Services (EDS) repository.
 * Supports dual-mode scanning:
 * 1. Local SDK mode: scans local eds/ folder directly on disk.
 * 2. Cloud Service mode: connects to GitHub via {@link GitHubClient} to fetch remote repository files.
 */
@Component(service = {KnowledgeSource.class, EDSRepositoryKnowledgeSource.class}, immediate = true)
public class EDSRepositoryKnowledgeSource implements KnowledgeSource {

    private static final Logger LOG = LoggerFactory.getLogger(EDSRepositoryKnowledgeSource.class);
    public static final String SOURCE_ID = "eds-repository";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private transient GitHubClient gitHubClient;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "md", "mdx", "json", "yaml", "yml", "js", "ts", "css", "html"
    );

    private static final List<String> EXCLUDED_PATH_FRAGMENTS = List.of(
            "node_modules", "dist", "build", ".git", ".husky", ".vscode",
            "package-lock.json", ".env", "target"
    );

    public EDSRepositoryKnowledgeSource() {
    }

    public EDSRepositoryKnowledgeSource(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @Override
    public String getId() {
        return SOURCE_ID;
    }

    @Override
    public String getName() {
        return "Edge Delivery Services Repository";
    }

    @Override
    public List<KnowledgeDocument> scan(KnowledgeSyncContext context) {
        List<KnowledgeDocument> documents = new ArrayList<>();
        String projectId = context != null ? context.getProjectId() : "default";

        // 1. Try Local Filesystem first if path is specified or standard path exists
        Path localPath = resolveLocalPath(context);
        if (localPath != null && Files.isDirectory(localPath)) {
            LOG.info("Scanning EDS knowledge from local directory: {}", localPath);
            scanLocalDirectory(localPath, localPath, projectId, documents);
            return documents;
        }

        // 2. Fall back to GitHubClient (Production / Cloud Service mode)
        if (gitHubClient != null && gitHubClient.testConnection()) {
            String branch = context != null && context.getBranch() != null ? context.getBranch() : gitHubClient.getDefaultBranch();
            LOG.info("Scanning EDS knowledge from remote GitHub repository on branch: {}", branch);
            scanRemoteGitHub(branch, projectId, documents);
            return documents;
        }

        LOG.warn("No valid local path or active GitHub connection found for EDS Knowledge Source.");
        return documents;
    }

    @Override
    public KnowledgeDocument read(KnowledgeSyncContext context, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String projectId = context != null ? context.getProjectId() : "default";

        Path localPath = resolveLocalPath(context);
        if (localPath != null && Files.isDirectory(localPath)) {
            Path filePath = localPath.resolve(relativePath).normalize();
            if (Files.isRegularFile(filePath)) {
                try {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    return buildDocument(relativePath, content, projectId, localPath.toString());
                } catch (IOException e) {
                    LOG.error("Failed to read local EDS file: {}", filePath, e);
                }
            }
        }

        if (gitHubClient != null) {
            String branch = context != null && context.getBranch() != null ? context.getBranch() : gitHubClient.getDefaultBranch();
            String content = gitHubClient.getFileContent(branch, relativePath);
            if (content != null) {
                return buildDocument(relativePath, content, projectId, gitHubClient.getRepoUrl());
            }
        }

        return null;
    }

    private Path resolveLocalPath(KnowledgeSyncContext context) {
        if (context != null && context.getLocalPath() != null && !context.getLocalPath().isBlank()) {
            Path p = Path.of(context.getLocalPath());
            if (Files.isDirectory(p)) {
                return p.toAbsolutePath().normalize();
            }
        }

        // Check common local repository paths
        List<Path> candidateRoots = List.of(
                Path.of("eds/wknd-site-abc"),
                Path.of("D:/eds personal/AEM-EDS-Modernizer/eds/wknd-site-abc"),
                Path.of("../eds/wknd-site-abc"),
                Path.of("eds")
        );

        for (Path c : candidateRoots) {
            if (Files.isDirectory(c)) {
                return c.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private void scanLocalDirectory(Path root, Path current, String projectId, List<KnowledgeDocument> out) {
        try (Stream<Path> stream = Files.list(current)) {
            for (Path path : stream.collect(Collectors.toList())) {
                String normalizedPath = path.toString().replace('\\', '/');
                if (isExcluded(normalizedPath)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    scanLocalDirectory(root, path, projectId, out);
                } else if (Files.isRegularFile(path) && isAllowed(path.getFileName().toString())) {
                    try {
                        String relative = root.relativize(path).toString().replace('\\', '/');
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        if (content == null || content.isBlank()) {
                            continue;
                        }
                        KnowledgeDocument doc = buildDocument(relative, content, projectId, root.toString());
                        out.add(doc);
                    } catch (Exception e) {
                        LOG.warn("Failed reading EDS file for indexing: {}", path, e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error listing directory: {}", current, e);
        }
    }

    private void scanRemoteGitHub(String branch, String projectId, List<KnowledgeDocument> out) {
        List<String> files = gitHubClient.listFilePaths(branch, "");
        for (String filePath : files) {
            if (isExcluded(filePath) || !isAllowed(filePath)) {
                continue;
            }
            try {
                String content = gitHubClient.getFileContent(branch, filePath);
                if (content != null && !content.isBlank()) {
                    KnowledgeDocument doc = buildDocument(filePath, content, projectId, gitHubClient.getRepoUrl());
                    out.add(doc);
                }
            } catch (Exception e) {
                LOG.warn("Failed reading GitHub file for indexing: {}", filePath, e);
            }
        }
    }

    private KnowledgeDocument buildDocument(String relativePath, String content, String projectId, String repo) {
        String docId = "eds:" + projectId + ":" + relativePath;
        String title = deriveTitle(relativePath, content);
        String docType = classifyDocumentType(relativePath);
        String mimeType = detectMimeType(relativePath);

        KnowledgeDocument doc = new KnowledgeDocument(docId, SOURCE_ID, relativePath, title);
        doc.setContent(content);
        doc.setRepository(repo);
        doc.setDocumentType(docType);
        doc.setMimeType(mimeType);
        doc.setFingerprint(FingerprintUtil.sha256(content));
        doc.setStatus("DISCOVERED");

        KnowledgeMetadata meta = new KnowledgeMetadata(projectId, "INTERNAL", docType);
        meta.setGlobal(false);
        meta.addAttribute("filePath", relativePath);
        meta.addAttribute("extension", getExtension(relativePath));
        doc.setMetadata(meta);

        return doc;
    }

    public static boolean isAllowed(String fileName) {
        String ext = getExtension(fileName);
        return ext != null && ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static boolean isExcluded(String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String frag : EXCLUDED_PATH_FRAGMENTS) {
            if (normalized.contains("/" + frag + "/") || normalized.contains("/" + frag) || normalized.endsWith(frag)) {
                return true;
            }
        }
        return false;
    }

    public static String classifyDocumentType(String path) {
        String norm = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (norm.contains("blocks/")) {
            return "EDS_BLOCK";
        }
        if (norm.endsWith("component-models.json") || norm.endsWith("component-definition.json") || norm.endsWith("component-filters.json")) {
            return "EDS_MODEL";
        }
        if (norm.endsWith(".md") || norm.endsWith(".mdx")) {
            return "MARKDOWN";
        }
        if (norm.endsWith(".yaml") || norm.endsWith(".yml") || norm.endsWith(".json")) {
            return "CONFIG";
        }
        if (norm.endsWith(".js") || norm.endsWith(".ts")) {
            return "SCRIPT";
        }
        if (norm.endsWith(".css")) {
            return "STYLE";
        }
        return "GENERAL";
    }

    public static String detectMimeType(String path) {
        String ext = getExtension(path);
        if (ext == null) return "text/plain";
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "md":
            case "mdx":
                return "text/markdown";
            case "json":
                return "application/json";
            case "yaml":
            case "yml":
                return "application/x-yaml";
            case "js":
                return "application/javascript";
            case "ts":
                return "application/typescript";
            case "css":
                return "text/css";
            case "html":
                return "text/html";
            default:
                return "text/plain";
        }
    }

    private static String deriveTitle(String path, String content) {
        if (path.endsWith(".md") && content != null) {
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.startsWith("# ")) {
                    return line.substring(2).trim();
                }
            }
        }
        return Path.of(path).getFileName().toString();
    }

    private static String getExtension(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('.');
        if (idx >= 0 && idx < path.length() - 1) {
            return path.substring(idx + 1);
        }
        return null;
    }
}
