package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a source document indexed from the EDS repository, documentation, or configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String sourceId;
    private String repository;
    private String path;
    private String title;
    private String mimeType;
    private String documentType; // MARKDOWN, EDS_BLOCK, EDS_MODEL, CONFIG_YAML, SCRIPT, ARCHITECTURE
    private String topic;
    private String language = "en";
    private String version = "1.0";
    private String fingerprint;
    private String content;
    private String createdAt;
    private String updatedAt;
    private String indexedAt;
    private String status = "DISCOVERED"; // DISCOVERED, PARSED, INDEXED, SKIPPED, ERROR
    private KnowledgeMetadata metadata = new KnowledgeMetadata();
    private List<KnowledgeChunk> chunks = new ArrayList<>();

    public KnowledgeDocument() {
        this.createdAt = Instant.now().toString();
        this.updatedAt = this.createdAt;
    }

    public KnowledgeDocument(String id, String sourceId, String path, String title) {
        this();
        this.id = id;
        this.sourceId = sourceId;
        this.path = path;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(String indexedAt) {
        this.indexedAt = indexedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public KnowledgeMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(KnowledgeMetadata metadata) {
        this.metadata = metadata;
    }

    public List<KnowledgeChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<KnowledgeChunk> chunks) {
        this.chunks = chunks != null ? chunks : new ArrayList<>();
    }

    public void addChunk(KnowledgeChunk chunk) {
        if (this.chunks == null) {
            this.chunks = new ArrayList<>();
        }
        this.chunks.add(chunk);
    }
}
