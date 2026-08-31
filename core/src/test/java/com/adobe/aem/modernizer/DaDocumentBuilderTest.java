package com.adobe.aem.modernizer;

import com.adobe.aem.modernizer.dashboard.DaDocumentBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DaDocumentBuilderTest {

    @Test
    void tableFirstRowIsBlockNameWithColspan() {
        String md = "# About Us\n\n### Hero\n| Image | Heading | Text |\n| --- | --- | --- |\n| /content/dam/wknd/hero.jpg | About Us | Explore |\n";
        String html = DaDocumentBuilder.fromMarkdown(md, "About Us", "/content/wknd/language-masters/en/about-us");

        assertThat(html).contains("<body>");
        assertThat(html).contains("<header></header>");
        assertThat(html).contains("<footer></footer>");
        assertThat(html).contains("<h1>About Us</h1>");
        assertThat(html).contains("<td colspan=\"2\">Hero</td>");
        assertThat(html).contains("<td colspan=\"2\">Metadata</td>");
        assertThat(html).contains("<td>title</td><td>About Us</td>");
        assertThat(html).contains("/content/wknd/language-masters/en/about-us");
        assertThat(html).doesNotContain("style=");
        assertThat(DaDocumentBuilder.documentPath("/content/wknd/language-masters/en/about-us"))
                .isEqualTo("/language-masters/en/about-us");
        assertThat(DaDocumentBuilder.pastePayload(html)).startsWith("<div>");
        assertThat(DaDocumentBuilder.pastePayload(html)).doesNotContain("<body>");
    }
}