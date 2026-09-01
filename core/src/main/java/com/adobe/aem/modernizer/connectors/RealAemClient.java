package com.adobe.aem.modernizer.connectors;

import com.adobe.aem.modernizer.persistence.model.SiteInventory;
import com.adobe.aem.modernizer.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Real AEM Live JCR Crawler (ADR 0005, Master §2).
 * Dynamically queries AEM Author via JCR REST endpoints (/content/...infinity.json)
 * to discover actual pages, child nodes, and exact component resource types.
 */
@Component(service = AemClient.class, immediate = true)
@ServiceRanking(100)
public class RealAemClient implements AemClient {

    private static final Logger LOG = LoggerFactory.getLogger(RealAemClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private String authorUrl = "http://localhost:4502";
    private String credentials = "admin:admin";

    public RealAemClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public RealAemClient(String authorUrl, String credentials) {
        this();
        if (authorUrl != null && !authorUrl.isEmpty()) {
            this.authorUrl = authorUrl.replaceAll("/+$", "");
        }
        if (credentials != null && !credentials.isEmpty()) {
            this.credentials = credentials;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(authorUrl + "/libs/granite/core/content/login.html"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() >= 200 && resp.statusCode() < 500;
        } catch (Exception e) {
            LOG.warn("AEM connection test failed to {}: {}", authorUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public SiteInventory crawl(String contentRoot, String pageScope) {
        return crawl(contentRoot, pageScope, "RECURSIVE");
    }

    @Override
    public SiteInventory crawl(String contentRoot, String pageScope, String scopeMode) {
        SiteInventory inv = new SiteInventory();
        String targetPath = (pageScope != null && !pageScope.trim().isEmpty())
                ? pageScope.trim().replaceAll("/\\*+$", "")
                : (contentRoot != null ? contentRoot.trim() : "/content/wknd");

        // Normalize path
        targetPath = targetPath.replaceAll("/+$", "").replace(".html", "");
        String mode = (scopeMode != null && !scopeMode.trim().isEmpty()) ? scopeMode.trim().toUpperCase() : "RECURSIVE";
        LOG.info("RealAemClient crawling live JCR at targetPath: {}, scopeMode: {}", targetPath, mode);

        try {
            // 1. Discover pages based on scopeMode ("SINGLE_PAGE" vs "DIRECT_CHILDREN" vs "RECURSIVE")
            List<String> pagePaths;
            if ("SINGLE_PAGE".equals(mode) || "THIS_PAGE".equals(mode) || "SINGLE".equals(mode)) {
                pagePaths = Collections.singletonList(targetPath);
            } else if ("DIRECT_CHILDREN".equals(mode) || "SHALLOW".equals(mode) || "1_LEVEL".equals(mode)) {
                pagePaths = discoverDirectChildPages(targetPath);
            } else {
                pagePaths = discoverAllPagePaths(targetPath);
            }

            if (pagePaths.isEmpty()) {
                pagePaths = Collections.singletonList(targetPath);
            }

            // 2. Crawl full component hierarchy for each discovered page
            List<SiteInventory.PageInfo> pages = new ArrayList<>();
            Set<String> distinctResourceTypes = new LinkedHashSet<>();
            Set<String> distinctAssetPaths = new LinkedHashSet<>();
            Set<String> distinctFragmentPaths = new LinkedHashSet<>();
            Set<String> distinctTemplates = new LinkedHashSet<>();

            for (String pPath : pagePaths) {
                JsonNode pageJson = fetchJcrJson(pPath + "/jcr:content.infinity.json");
                if (pageJson == null) {
                    pageJson = fetchJcrJson(pPath + ".infinity.json");
                }

                String title = formatTitleFromPath(pPath);
                String template = "/conf/wknd/settings/wcm/templates/page";

                List<String> componentTypes = new ArrayList<>();
                List<String> assetPaths = new ArrayList<>();

                if (pageJson != null) {
                    JsonNode jcrContent = pageJson.has("jcr:content") ? pageJson.get("jcr:content") : pageJson;
                    if (jcrContent.has("jcr:title") && !jcrContent.get("jcr:title").asText().isEmpty()) {
                        title = jcrContent.get("jcr:title").asText();
                    } else if (jcrContent.has("pageTitle") && !jcrContent.get("pageTitle").asText().isEmpty()) {
                        title = jcrContent.get("pageTitle").asText();
                    }
                    if (jcrContent.has("cq:template")) {
                        template = jcrContent.get("cq:template").asText();
                    }

                    // Recursively extract all sling:resourceType, fileReference, and fragmentPath
                    extractJcrDetails(jcrContent, componentTypes, assetPaths, distinctFragmentPaths);
                }

                SiteInventory.PageInfo pageInfo = new SiteInventory.PageInfo(pPath, title, template);
                pageInfo.setComponentResourceTypes(componentTypes);
                pageInfo.setAssetPaths(assetPaths);
                pages.add(pageInfo);

                distinctResourceTypes.addAll(componentTypes);
                distinctAssetPaths.addAll(assetPaths);
                distinctTemplates.add(template);
            }

            inv.setTotalPages(pages.size());
            inv.setEligiblePages(pages.size());
            inv.setExcludedPages(0);
            inv.setPages(pages);

            // 3. Map distinct discovered components
            List<SiteInventory.ComponentInfo> components = new ArrayList<>();
            for (String rt : distinctResourceTypes) {
                String simpleName = rt.substring(rt.lastIndexOf('/') + 1);
                String formattedTitle = Character.toUpperCase(simpleName.charAt(0)) + simpleName.substring(1).replace('-', ' ');
                SiteInventory.ComponentInfo ci = new SiteInventory.ComponentInfo(rt, formattedTitle, "WKND Content");
                ci.setProposedEdsBlock(simpleName.toLowerCase().replace(' ', '-'));
                components.add(ci);
            }
            inv.setComponents(components);

            // 4. Templates
            List<SiteInventory.TemplateInfo> templates = new ArrayList<>();
            for (String tPath : distinctTemplates) {
                String tTitle = formatTitleFromPath(tPath);
                templates.add(new SiteInventory.TemplateInfo(tPath, tTitle));
            }
            inv.setTemplates(templates);

            // 5. Assets
            List<SiteInventory.AssetInfo> assets = new ArrayList<>();
            for (String aPath : distinctAssetPaths) {
                assets.add(new SiteInventory.AssetInfo(aPath, "image/jpeg"));
            }
            inv.setAssets(assets);

            // 6. Content Fragments
            List<SiteInventory.ContentFragmentInfo> cfs = new ArrayList<>();
            for (String cfPath : distinctFragmentPaths) {
                String cfTitle = formatTitleFromPath(cfPath);
                cfs.add(new SiteInventory.ContentFragmentInfo(cfPath, "adventure-model", cfTitle));
            }
            inv.setContentFragments(cfs);

            LOG.info("RealAemClient finished crawling {}: {} pages, {} components mapped",
                    targetPath, pages.size(), components.size());

        } catch (Exception e) {
            LOG.error("Failed crawling live AEM instance at {}: {}", targetPath, e.getMessage(), e);
        }

        return inv;
    }

    private void extractJcrDetails(JsonNode node, List<String> componentTypes, List<String> assetPaths, Set<String> fragmentPaths) {
        if (node == null || !node.isObject()) return;

        // Check sling:resourceType
        if (node.has("sling:resourceType")) {
            String rt = node.get("sling:resourceType").asText();
            // Filter out internal structural layout containers
            if (isEligibleComponentType(rt)) {
                if (!componentTypes.contains(rt)) {
                    componentTypes.add(rt);
                }
            }
        }

        // Check image asset references
        if (node.has("fileReference")) {
            String fileRef = node.get("fileReference").asText();
            if (fileRef.startsWith("/content/dam/") && !assetPaths.contains(fileRef)) {
                assetPaths.add(fileRef);
            }
        }

        // Check Content Fragment references
        if (node.has("fragmentPath")) {
            String fragPath = node.get("fragmentPath").asText();
            if (fragPath.startsWith("/content/dam/")) {
                fragmentPaths.add(fragPath);
            }
        }

        // Recurse child nodes
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            JsonNode child = node.get(field);
            if (child.isObject()) {
                extractJcrDetails(child, componentTypes, assetPaths, fragmentPaths);
            }
        }
    }

    private boolean isEligibleComponentType(String rt) {
        if (rt == null || rt.isEmpty()) return false;
        // Ignore generic Sling / JCR foundation containers
        if (rt.equals("wcm/foundation/components/responsivegrid")
                || rt.equals("dam/cfm/components/grid")
                || rt.equals("nt:unstructured")
                || rt.equals("cq/gui/components/coral/common/form/page")) {
            return false;
        }
        return true;
    }

    private List<String> discoverDirectChildPages(String targetPath) {
        Set<String> pagePaths = new LinkedHashSet<>();
        JsonNode rootJson = fetchJcrJson(targetPath + ".1.json");
        if (rootJson != null) {
            boolean isCurrentNodePage = rootJson.has("jcr:content")
                    || (rootJson.has("jcr:primaryType") && "cq:Page".equals(rootJson.get("jcr:primaryType").asText()));
            if (isCurrentNodePage) {
                pagePaths.add(targetPath);
            }
            Iterator<String> fieldNames = rootJson.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (field.startsWith("jcr:") || field.startsWith("sling:") || field.equals("jcr:content")) {
                    continue;
                }
                JsonNode child = rootJson.get(field);
                if (child.isObject()) {
                    if (child.has("jcr:primaryType") && "cq:Page".equals(child.get("jcr:primaryType").asText())
                            || child.has("jcr:content")) {
                        pagePaths.add(targetPath + "/" + field);
                    }
                }
            }
        }
        if (pagePaths.isEmpty()) {
            pagePaths.add(targetPath);
        }
        return new ArrayList<>(pagePaths);
    }

    private List<String> discoverAllPagePaths(String targetPath) {
        Set<String> pagePaths = new LinkedHashSet<>();

        // 1. Try AEM QueryBuilder API for fast, indexed recursive cq:Page search
        try {
            String qbUrl = authorUrl + "/bin/querybuilder.json?path=" + targetPath + "&path.self=true&type=cq:Page&p.limit=-1";
            JsonNode qbJson = fetchUrlJson(qbUrl);
            if (qbJson != null && qbJson.has("hits") && qbJson.get("hits").isArray() && qbJson.get("hits").size() > 0) {
                for (JsonNode hit : qbJson.get("hits")) {
                    String p = hit.has("path") ? hit.get("path").asText() : (hit.has("jcr:path") ? hit.get("jcr:path").asText() : null);
                    if (p != null && !p.isEmpty()) {
                        pagePaths.add(p);
                    }
                }
                if (!pagePaths.isEmpty()) {
                    LOG.info("QueryBuilder discovered {} pages under {}", pagePaths.size(), targetPath);
                    return new ArrayList<>(pagePaths);
                }
            }
        } catch (Exception e) {
            LOG.warn("QueryBuilder search failed for {}: {}", targetPath, e.getMessage());
        }

        // 2. Fallback: Recursive JCR tree traversal
        LOG.info("Falling back to recursive JCR traversal for {}", targetPath);
        discoverPagesRecursively(targetPath, pagePaths, new HashSet<>());
        if (pagePaths.isEmpty()) {
            pagePaths.add(targetPath);
        }
        return new ArrayList<>(pagePaths);
    }

    private void discoverPagesRecursively(String path, Set<String> pagePaths, Set<String> visited) {
        if (path == null || path.isEmpty() || visited.contains(path)) return;
        visited.add(path);

        JsonNode json = fetchJcrJson(path + ".1.json");
        if (json == null) return;

        boolean isPage = json.has("jcr:content")
                || (json.has("jcr:primaryType") && "cq:Page".equals(json.get("jcr:primaryType").asText()));
        if (isPage) {
            pagePaths.add(path);
        }

        Iterator<String> fieldNames = json.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (field.startsWith("jcr:") || field.startsWith("sling:") || field.equals("jcr:content")) {
                continue;
            }
            JsonNode child = json.get(field);
            if (child.isObject()) {
                boolean childIsPage = child.has("jcr:content")
                        || (child.has("jcr:primaryType") && "cq:Page".equals(child.get("jcr:primaryType").asText()));
                if (childIsPage) {
                    discoverPagesRecursively(path + "/" + field, pagePaths, visited);
                }
            }
        }
    }

    private JsonNode fetchUrlJson(String url) {
        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().trim().startsWith("{")) {
                return MAPPER.readTree(resp.body());
            }
        } catch (Exception e) {
            LOG.debug("Error fetching URL {}: {}", url, e.getMessage());
        }
        return null;
    }

    private JsonNode fetchJcrJson(String path) {
        try {
            String url = authorUrl + (path.startsWith("/") ? path : ("/" + path));
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().trim().startsWith("{")) {
                return MAPPER.readTree(resp.body());
            }
        } catch (Exception e) {
            LOG.debug("Error fetching JCR path {}: {}", path, e.getMessage());
        }
        return null;
    }

    private String formatTitleFromPath(String path) {
        if (path == null || path.isEmpty()) return "Untitled";
        String name = path.substring(path.lastIndexOf('/') + 1);
        String[] parts = name.replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public String getAuthorUrl() {
        return authorUrl;
    }

    @Override
    public String getRole() {
        return "author";
    }
}
