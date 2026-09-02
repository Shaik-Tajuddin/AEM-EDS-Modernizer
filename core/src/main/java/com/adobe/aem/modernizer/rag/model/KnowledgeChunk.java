package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a discrete, semantically-chunked unit of knowledge.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    private String chunkId;
    private String documentId;
    private String sourceId;
    private String repository;
    private String path;
    private String content;
    private String heading;
    private String section;
    private String chunkType; // MARKDOWN_SECTION, EDS_BLOCK_JS, EDS_MODEL_JSON, YAML_CONFIG, CODE
    private int tokenCount;
    private int startLine;
    private int endLine;
    private String fingerprint;
    private String embeddingModel;
    private String embeddingVersion;
    private KnowledgeMetadata metadata = new KnowledgeMetadata();
    private Map<String, Object> properties = new LinkedHashMap<>();

    public KnowledgeChunk() {
    }

    public KnowledgeChunk(String chunkId, String documentId, String content) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.content = content;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getChunkType() {
        return chunkType;
    }

    public void setChunkType(String chunkType) {
        this.chunkType = chunkType;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    public KnowledgeMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(KnowledgeMetadata metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? new LinkedHashMap<>(properties) : new LinkedHashMap<>();
    }
}
