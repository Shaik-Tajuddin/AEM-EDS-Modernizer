package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;

/**
 * Tracks the state and metrics of an ingestion sync execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeSyncRun implements Serializable {

    private static final long serialVersionUID = 1L;

    private String syncId;
    private String projectId;
    private String sourceId;
    private String status = "CREATED"; // CREATED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    private int documentsDiscovered;
    private int documentsProcessed;
    private int documentsSkipped;
    private int documentsFailed;
    private int chunksCreated;
    private int embeddingsCreated;
    private String currentDocument;
    private String lastCheckpoint;
    private String startTime;
    private String endTime;
    private String errorMessage;

    public KnowledgeSyncRun() {
        this.startTime = Instant.now().toString();
    }

    public KnowledgeSyncRun(String syncId, String projectId, String sourceId) {
        this();
        this.syncId = syncId;
        this.projectId = projectId;
        this.sourceId = sourceId;
    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDocumentsDiscovered() {
        return documentsDiscovered;
    }

    public void setDocumentsDiscovered(int documentsDiscovered) {
        this.documentsDiscovered = documentsDiscovered;
    }

    public int getDocumentsProcessed() {
        return documentsProcessed;
    }

    public void setDocumentsProcessed(int documentsProcessed) {
        this.documentsProcessed = documentsProcessed;
    }

    public int getDocumentsSkipped() {
        return documentsSkipped;
    }

    public void setDocumentsSkipped(int documentsSkipped) {
        this.documentsSkipped = documentsSkipped;
    }

    public int getDocumentsFailed() {
        return documentsFailed;
    }

    public void setDocumentsFailed(int documentsFailed) {
        this.documentsFailed = documentsFailed;
    }

    public int getChunksCreated() {
        return chunksCreated;
    }

    public void setChunksCreated(int chunksCreated) {
        this.chunksCreated = chunksCreated;
    }

    public int getEmbeddingsCreated() {
        return embeddingsCreated;
    }

    public void setEmbeddingsCreated(int embeddingsCreated) {
        this.embeddingsCreated = embeddingsCreated;
    }

    public String getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(String currentDocument) {
        this.currentDocument = currentDocument;
    }

    public String getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(String lastCheckpoint) {
        this.lastCheckpoint = lastCheckpoint;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
