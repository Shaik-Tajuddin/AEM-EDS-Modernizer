package com.adobe.aem.modernizer.rag.source;

import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncContext;

import java.util.List;

/**
 * Common abstraction for pluggable knowledge sources (EDS repository, AEM JCR, Adobe Docs, Figma, ADRs).
 */
public interface KnowledgeSource {

    /** Unique identifier for the source (e.g., "eds-repository", "aem-jcr", "migration-history"). */
    String getId();

    /** Display name for dashboards and logging. */
    String getName();

    /** Scans the source given the sync context and returns discovered documents. */
    List<KnowledgeDocument> scan(KnowledgeSyncContext context);

    /** Reads a single document from this source by relative or canonical path. */
    KnowledgeDocument read(KnowledgeSyncContext context, String path);
}
