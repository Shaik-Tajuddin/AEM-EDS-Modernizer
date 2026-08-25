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
        SiteInventory inv = new SiteInventory();
        String targetPath = (pageScope != null && !pageScope.trim().isEmpty() && !pageScope.endsWith("/*"))
                ? pageScope.trim()
                : (contentRoot != null ? contentRoot.trim() : "/content/wknd");

        // Normalize path
        targetPath = targetPath.replaceAll("/+$", "").replace(".html", "");
        LOG.info("RealAemClient crawling live JCR at targetPath: {}", targetPath);

        try {
            // 1. Fetch 1-level structure to check if there are child pages
            JsonNode rootJson = fetchJcrJson(targetPath + ".1.json");
            if (rootJson == null) {
                LOG.warn("Could not fetch JCR data for path {}, attempting infinity fallback", targetPath);
                rootJson = fetchJcrJson(targetPath + ".infinity.json");
            }

            List<String> pagePaths = new ArrayList<>();
            if (rootJson != null) {
                // Check if this node itself is a page (has jcr:content or jcr:primaryType == cq:Page)
                boolean isCurrentNodePage = rootJson.has("jcr:content")
                        || (rootJson.has("jcr:primaryType") && "cq:Page".equals(rootJson.get("jcr:primaryType").asText()));

                // Check for child cq:Page nodes
                List<String> childPageNames = new ArrayList<>();
                Iterator<String> fieldNames = rootJson.fieldNames();
                while (fieldNames.hasNext()) {
                    String field = fieldNames.next();
                    if (field.startsWith("jcr:") || field.startsWith("sling:") || field.equals("jcr:content")) {
                        continue;
                    }
                    JsonNode child = rootJson.get(field);
                    if (child.isObject()) {
                        if (child.has("jcr:primaryType") && "cq:Page".equals(child.get("jcr:primaryType").asText())) {
                            childPageNames.add(field);
                        } else if (child.has("jcr:content")) {
                            childPageNames.add(field);
                        }
                    }
                }

                if (childPageNames.isEmpty() && isCurrentNodePage) {
                    // Leaf Single Page!
                    LOG.info("Target path {} has no child pages. Discovered as SINGLE PAGE.", targetPath);
                    pagePaths.add(targetPath);
                } else {
                    if (isCurrentNodePage) {
                        pagePaths.add(targetPath);
                    }
                    for (String childName : childPageNames) {
                        pagePaths.add(targetPath + "/" + childName);
                    }
                }
            }

            if (pagePaths.isEmpty()) {
                pagePaths.add(targetPath);
            }

            // 2. Crawl full component hierarchy for each discovered page
            List<SiteInventory.PageInfo> pages = new ArrayList<>();
            Set<String> distinctResourceTypes = new LinkedHashSet<>();
            Set<String> distinctAssetPaths = new LinkedHashSet<>();
            Set<String> distinctFragmentPaths = new LinkedHashSet<>();
            Set<String> distinctTemplates = new LinkedHashSet<>();

            for (String pPath : pagePaths) {
                JsonNode pageJson = fetchJcrJson(pPath + ".infinity.json");
                String title = formatTitleFromPath(pPath);
                String template = "/conf/wknd/settings/wcm/templates/page";

                List<String> componentTypes = new ArrayList<>();
                List<String> assetPaths = new ArrayList<>();

                if (pageJson != null) {
                    JsonNode jcrContent = pageJson.has("jcr:content") ? pageJson.get("jcr:content") : pageJson;
                    if (jcrContent.has("jcr:title")) {
                        title = jcrContent.get("jcr:title").asText();
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
