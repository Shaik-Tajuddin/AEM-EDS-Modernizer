package com.adobe.aem.modernizer.rag.retrieval;

import com.adobe.aem.modernizer.rag.model.Citation;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.RetrievalResult;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Citation generation and validation service (Section 24).
 * Guarantees that citations strictly correspond to retrieved source documents and are never fabricated.
 */
@Component(service = CitationService.class, immediate = true)
public class CitationService {

    public List<Citation> buildCitations(List<RetrievalResult> rankedResults) {
        if (rankedResults == null || rankedResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Citation> citations = new ArrayList<>();
        int index = 1;
        for (RetrievalResult res : rankedResults) {
            KnowledgeChunk chunk = res.getChunk();
            if (chunk == null) continue;

            Citation c = new Citation(index++, chunk.getPath(), chunk.getSection(), res.getCombinedScore());
            c.setSource(chunk.getSourceId() != null ? chunk.getSourceId() : "eds-repository");
            c.setRepository(chunk.getRepository());
            c.setDocumentId(chunk.getDocumentId());
            c.setChunkId(chunk.getChunkId());
            c.setStartLine(chunk.getStartLine());
            c.setEndLine(chunk.getEndLine());

            String text = chunk.getContent();
            c.setSnippet(text != null && text.length() > 220 ? text.substring(0, 220) + "..." : text);
            citations.add(c);
            res.setCitation(c);
        }

        return citations;
    }

    public String formatCitationMarkdown(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n**Sources:**\n");
        for (Citation c : citations) {
            sb.append(c.getIndex()).append(". `").append(c.getPath() != null ? c.getPath() : "repository");
            if (c.getStartLine() > 0) {
                sb.append("#L").append(c.getStartLine()).append("-L").append(c.getEndLine());
            }
            sb.append("`");
            if (c.getSection() != null && !c.getSection().isBlank()) {
                sb.append(" (").append(c.getSection()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
