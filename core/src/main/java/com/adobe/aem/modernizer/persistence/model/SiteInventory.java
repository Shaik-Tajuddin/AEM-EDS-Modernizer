package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovered Site Inventory from AEM crawler.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteInventory {

    private String projectId;
    private String jobId;
    private int totalPages;
    private int eligiblePages;
    private int excludedPages;
    private List<PageInfo> pages = new ArrayList<>();
    private List<ComponentInfo> components = new ArrayList<>();
    private List<TemplateInfo> templates = new ArrayList<>();
    private List<AssetInfo> assets = new ArrayList<>();
    private List<ContentFragmentInfo> contentFragments = new ArrayList<>();
    private List<MsmLiveCopyInfo> liveCopies = new ArrayList<>();
    private Map<String, String> figmaTokens = new HashMap<>();
    private long timestamp;

    public SiteInventory() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getEligiblePages() { return eligiblePages; }
    public void setEligiblePages(int eligiblePages) { this.eligiblePages = eligiblePages; }

    public int getExcludedPages() { return excludedPages; }
    public void setExcludedPages(int excludedPages) { this.excludedPages = excludedPages; }

    public List<PageInfo> getPages() { return pages; }
    public void setPages(List<PageInfo> pages) { this.pages = pages; }

    public List<ComponentInfo> getComponents() { return components; }
    public void setComponents(List<ComponentInfo> components) { this.components = components; }

    public List<TemplateInfo> getTemplates() { return templates; }
    public void setTemplates(List<TemplateInfo> templates) { this.templates = templates; }

    public List<AssetInfo> getAssets() { return assets; }
    public void setAssets(List<AssetInfo> assets) { this.assets = assets; }

    public List<ContentFragmentInfo> getContentFragments() { return contentFragments; }
    public void setContentFragments(List<ContentFragmentInfo> contentFragments) { this.contentFragments = contentFragments; }

    public List<MsmLiveCopyInfo> getLiveCopies() { return liveCopies; }
    public void setLiveCopies(List<MsmLiveCopyInfo> liveCopies) { this.liveCopies = liveCopies; }

    public Map<String, String> getFigmaTokens() { return figmaTokens; }
    public void setFigmaTokens(Map<String, String> figmaTokens) { this.figmaTokens = figmaTokens; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Helpers for estimator
    public int pages() { return eligiblePages > 0 ? eligiblePages : pages.size(); }
    public int components() { return components.size(); }
    public int distinctBlocks() { return components.size(); }
    public int figmaFiles() { return figmaTokens.isEmpty() ? 0 : 1; }

    // Inner DTOs
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        private String path;
        private String title;
        private String template;
        private boolean eligible = true;
        private String exclusionReason;
        private List<String> componentResourceTypes = new ArrayList<>();
        private List<String> assetPaths = new ArrayList<>();

        public PageInfo() {}
        public PageInfo(String path, String title, String template) {
            this.path = path;
            this.title = title;
            this.template = template;
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }

        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }

        public String getExclusionReason() { return exclusionReason; }
        public void setExclusionReason(String exclusionReason) { this.exclusionReason = exclusionReason; }

        public List<String> getComponentResourceTypes() { return componentResourceTypes; }
        public void setComponentResourceTypes(List<String> componentResourceTypes) { this.componentResourceTypes = componentResourceTypes; }

        public List<String> getAssetPaths() { return assetPaths; }
        public void setAssetPaths(List<String> assetPaths) { this.assetPaths = assetPaths; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentInfo {
        private String resourceType;
        private String title;
        private String group;
        private int occurrenceCount;
        private String proposedEdsBlock;
        private String capabilityClassification = "SUPPORTED"; // SUPPORTED, SUPPORTED_WITH_TRANSFORMATION, etc.

        public ComponentInfo() {}
        public ComponentInfo(String resourceType, String title, String group) {
            this.resourceType = resourceType;
            this.title = title;
            this.group = group;
        }

        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public int getOccurrenceCount() { return occurrenceCount; }
        public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }

        public String getProposedEdsBlock() { return proposedEdsBlock; }
        public void setProposedEdsBlock(String proposedEdsBlock) { this.proposedEdsBlock = proposedEdsBlock; }

        public String getCapabilityClassification() { return capabilityClassification; }
        public void setCapabilityClassification(String capabilityClassification) { this.capabilityClassification = capabilityClassification; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateInfo {
        private String path;
        private String title;
        private List<String> allowedComponents = new ArrayList<>();

        public TemplateInfo() {}
        public TemplateInfo(String path, String title) {
            this.path = path;
            this.title = title;
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public List<String> getAllowedComponents() { return allowedComponents; }
        public void setAllowedComponents(List<String> allowedComponents) { this.allowedComponents = allowedComponents; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetInfo {
        private String path;
        private String mimeType;
        private boolean resolvable = true;

        public AssetInfo() {}
        public AssetInfo(String path, String mimeType) {
            this.path = path;
            this.mimeType = mimeType;
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }

        public boolean isResolvable() { return resolvable; }
        public void setResolvable(boolean resolvable) { this.resolvable = resolvable; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentFragmentInfo {
        private String path;
        private String model;
        private String title;

        public ContentFragmentInfo() {}
        public ContentFragmentInfo(String path, String model, String title) {
            this.path = path;
            this.model = model;
            this.title = title;
        }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MsmLiveCopyInfo {
        private String sourcePath;
        private String liveCopyPath;
        private String rollOutConfig;

        public MsmLiveCopyInfo() {}
        public MsmLiveCopyInfo(String sourcePath, String liveCopyPath) {
            this.sourcePath = sourcePath;
            this.liveCopyPath = liveCopyPath;
        }

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

        public String getLiveCopyPath() { return liveCopyPath; }
        public void setLiveCopyPath(String liveCopyPath) { this.liveCopyPath = liveCopyPath; }

        public String getRollOutConfig() { return rollOutConfig; }
        public void setRollOutConfig(String rollOutConfig) { this.rollOutConfig = rollOutConfig; }
    }
}
