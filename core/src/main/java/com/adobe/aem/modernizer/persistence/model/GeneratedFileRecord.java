package com.adobe.aem.modernizer.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Persisted record of a generated EDS file (JS, CSS, Markdown, JSON, YAML).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratedFileRecord {

    private String id;
    private String projectId;
    private String jobId;
    private String path; // e.g. "blocks/hero/hero.js", "content/wknd/en.md"
    private String fileType; // "BLOCK_JS", "BLOCK_CSS", "SECTION_MD", "CONFIG", "INDEX"
    private String content;
    private String sourcePath;
    private boolean virtualDiffOnly;
    private long createdAt;

    public GeneratedFileRecord() {
        this.createdAt = System.currentTimeMillis();
    }

    public GeneratedFileRecord(String id, String projectId, String jobId, String path, String fileType, String content) {
        this();
        this.id = id;
        this.projectId = projectId;
        this.jobId = jobId;
        this.path = path;
        this.fileType = fileType;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public boolean isVirtualDiffOnly() { return virtualDiffOnly; }
    public void setVirtualDiffOnly(boolean virtualDiffOnly) { this.virtualDiffOnly = virtualDiffOnly; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
