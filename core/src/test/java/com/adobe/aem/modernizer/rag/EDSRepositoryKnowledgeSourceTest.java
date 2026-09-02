package com.adobe.aem.modernizer.rag;

import com.adobe.aem.modernizer.rag.chunking.SemanticChunker;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeSyncContext;
import com.adobe.aem.modernizer.rag.source.EDSRepositoryKnowledgeSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EDSRepositoryKnowledgeSourceTest {

    @Test
    void testLocalEdsRepositoryScan() {
        EDSRepositoryKnowledgeSource source = new EDSRepositoryKnowledgeSource(null);

        KnowledgeSyncContext ctx = new KnowledgeSyncContext("sync-test-1", "wknd-site", EDSRepositoryKnowledgeSource.SOURCE_ID);
        File edsDir = new File("eds/wknd-site-abc");
        if (!edsDir.exists()) {
            edsDir = new File("d:/eds personal/AEM-EDS-Modernizer/eds/wknd-site-abc");
        }
        ctx.setLocalPath(edsDir.getAbsolutePath());

        List<KnowledgeDocument> docs = source.scan(ctx);

        assertThat(docs).isNotEmpty();
        boolean foundBlock = false;
        boolean foundModel = false;
        boolean foundMd = false;

        for (KnowledgeDocument doc : docs) {
            assertThat(doc.getFingerprint()).isNotEmpty();
            assertThat(doc.getContent()).isNotEmpty();
            if ("EDS_BLOCK".equals(doc.getDocumentType())) foundBlock = true;
            if ("EDS_MODEL".equals(doc.getDocumentType())) foundModel = true;
            if ("MARKDOWN".equals(doc.getDocumentType())) foundMd = true;
        }

        assertThat(foundBlock || foundModel || foundMd).isTrue();
    }

    @Test
    void testScanAndChunkingPipeline() {
        EDSRepositoryKnowledgeSource source = new EDSRepositoryKnowledgeSource(null);
        SemanticChunker chunker = new SemanticChunker();

        KnowledgeSyncContext ctx = new KnowledgeSyncContext("sync-test-2", "wknd-site", EDSRepositoryKnowledgeSource.SOURCE_ID);
        File edsDir = new File("eds/wknd-site-abc");
        ctx.setLocalPath(edsDir.getAbsolutePath());

        List<KnowledgeDocument> docs = source.scan(ctx);
        assertThat(docs).isNotEmpty();

        KnowledgeDocument sampleDoc = docs.get(0);
        List<KnowledgeChunk> chunks = chunker.chunk(sampleDoc);

        assertThat(chunks).isNotEmpty();
        for (KnowledgeChunk c : chunks) {
            assertThat(c.getChunkId()).isNotEmpty();
            assertThat(c.getContent()).isNotEmpty();
            assertThat(c.getMetadata().getProjectId()).isEqualTo("wknd-site");
        }
    }
}
