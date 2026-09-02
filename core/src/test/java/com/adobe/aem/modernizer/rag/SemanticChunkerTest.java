package com.adobe.aem.modernizer.rag;

import com.adobe.aem.modernizer.rag.chunking.SemanticChunker;
import com.adobe.aem.modernizer.rag.model.KnowledgeChunk;
import com.adobe.aem.modernizer.rag.model.KnowledgeDocument;
import com.adobe.aem.modernizer.rag.model.KnowledgeMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticChunkerTest {

    private final SemanticChunker chunker = new SemanticChunker();

    @Test
    void testMarkdownHeadingChunking() {
        String md = "# Hero Block Guide\n\n" +
                "The Hero block displays a full-bleed banner image with headline text and CTA.\n\n" +
                "## Authoring Rules\n\n" +
                "Authors should supply one image and one heading in the table cell.\n\n" +
                "## CSS Variants\n\n" +
                "Supported variants include `.dark` and `.centered`.\n";

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId("doc-md-1");
        doc.setPath("docs/hero.md");
        doc.setDocumentType("MARKDOWN");
        doc.setContent(md);
        doc.setMetadata(new KnowledgeMetadata("proj-1", "DOCS", "MARKDOWN"));

        List<KnowledgeChunk> chunks = chunker.chunk(doc);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).getHeading()).isEqualTo("Hero Block Guide");
        assertThat(chunks.get(1).getHeading()).isEqualTo("Authoring Rules");
        assertThat(chunks.get(2).getHeading()).isEqualTo("CSS Variants");
        assertThat(chunks.get(1).getContent()).contains("Authors should supply one image");
    }

    @Test
    void testJsDecoratorChunking() {
        String js = "export default function decorate(block) {\n" +
                "  const cols = [...block.firstElementChild.children];\n" +
                "  block.classList.add(`columns-${cols.length}-cols`);\n" +
                "}\n";

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId("doc-js-1");
        doc.setPath("blocks/columns/columns.js");
        doc.setDocumentType("BLOCK_JS");
        doc.setContent(js);
        doc.setMetadata(new KnowledgeMetadata("proj-1", "EDS_CODE", "BLOCK_JS"));

        List<KnowledgeChunk> chunks = chunker.chunk(doc);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getChunkType()).isEqualTo("EDS_JS_DECORATOR");
        assertThat(chunks.get(0).getContent()).contains("export default function decorate");
    }

    @Test
    void testCssChunking() {
        String css = ".hero {\n  position: relative;\n  min-height: 480px;\n}\n\n" +
                ".hero.dark {\n  background: #000;\n  color: #fff;\n}\n";

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId("doc-css-1");
        doc.setPath("blocks/hero/hero.css");
        doc.setDocumentType("BLOCK_CSS");
        doc.setContent(css);
        doc.setMetadata(new KnowledgeMetadata("proj-1", "EDS_CODE", "BLOCK_CSS"));

        List<KnowledgeChunk> chunks = chunker.chunk(doc);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getChunkType()).isEqualTo("EDS_CSS_RULES");
        assertThat(chunks.get(0).getContent()).contains(".hero");
    }

    @Test
    void testJsonModelChunking() {
        String json = "{\n" +
                "  \"definitions\": [\n" +
                "    { \"title\": \"Hero Block\", \"id\": \"hero\", \"plugins\": {} },\n" +
                "    { \"title\": \"Teaser Block\", \"id\": \"teaser\", \"plugins\": {} }\n" +
                "  ]\n" +
                "}";

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId("doc-json-1");
        doc.setPath("models/component-models.json");
        doc.setDocumentType("COMPONENT_DEFINITION");
        doc.setContent(json);
        doc.setMetadata(new KnowledgeMetadata("proj-1", "EDS_MODEL", "COMPONENT_DEFINITION"));

        List<KnowledgeChunk> chunks = chunker.chunk(doc);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getHeading()).contains("Hero Block");
        assertThat(chunks.get(1).getHeading()).contains("Teaser Block");
    }
}
