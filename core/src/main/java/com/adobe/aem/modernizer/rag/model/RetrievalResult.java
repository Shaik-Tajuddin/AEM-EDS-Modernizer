package com.adobe.aem.modernizer.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * Encapsulates a single retrieved chunk along with relevance scoring, source authority, and channel origin.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetrievalResult implements Serializable, Comparable<RetrievalResult> {

    private static final long serialVersionUID = 1L;

    private KnowledgeChunk chunk;
    private double score;
    private double authorityScore = 1.0;
    private double combinedScore;
    private String retrievalChannel; // SEMANTIC, KEYWORD, JCR_STRUCTURED, MIGRATION_HISTORY
    private Citation citation;

    public RetrievalResult() {
    }

    public RetrievalResult(KnowledgeChunk chunk, double score, String retrievalChannel) {
        this.chunk = chunk;
        this.score = score;
        this.retrievalChannel = retrievalChannel;
        this.combinedScore = score;
        if (chunk != null) {
            this.citation = new Citation(0, chunk.getPath(), chunk.getSection(), score);
            this.citation.setChunkId(chunk.getChunkId());
            this.citation.setDocumentId(chunk.getDocumentId());
            this.citation.setRepository(chunk.getRepository());
            this.citation.setStartLine(chunk.getStartLine());
            this.citation.setEndLine(chunk.getEndLine());
            this.citation.setSnippet(chunk.getContent() != null && chunk.getContent().length() > 200
                    ? chunk.getContent().substring(0, 200) + "..." : chunk.getContent());
        }
    }

    public KnowledgeChunk getChunk() {
        return chunk;
    }

    public void setChunk(KnowledgeChunk chunk) {
        this.chunk = chunk;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
        recalculateCombined();
    }

    public double getAuthorityScore() {
        return authorityScore;
    }

    public void setAuthorityScore(double authorityScore) {
        this.authorityScore = authorityScore;
        recalculateCombined();
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

    public String getRetrievalChannel() {
        return retrievalChannel;
    }

    public void setRetrievalChannel(String retrievalChannel) {
        this.retrievalChannel = retrievalChannel;
    }

    public Citation getCitation() {
        return citation;
    }

    public void setCitation(Citation citation) {
        this.citation = citation;
    }

    private void recalculateCombined() {
        this.combinedScore = this.score * this.authorityScore;
    }

    @Override
    public int compareTo(RetrievalResult o) {
        return Double.compare(o.combinedScore, this.combinedScore);
    }
}
