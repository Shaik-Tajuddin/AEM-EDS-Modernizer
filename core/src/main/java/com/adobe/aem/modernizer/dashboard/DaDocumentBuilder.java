package com.adobe.aem.modernizer.dashboard;

import com.adobe.aem.modernizer.services.UrlRedirectService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds Document Authoring (da.live) paste HTML: table-form blocks whose first
 * row is the block name with colspan. Property-header markdown tables are not DA.
 */
public final class DaDocumentBuilder {

    private DaDocumentBuilder() {}

    public static String documentPath(String aemPath) {
        String eds = UrlRedirectService.transformToEdsPath(aemPath);
        if (eds == null || eds.isBlank() || "/".equals(eds)) {
            return "/index";
        }
        return eds.startsWith("/") ? eds : "/" + eds;
    }

    public static String fromMarkdown(String markdown, String pageTitle, String aemPath) {
        String title = (pageTitle == null || pageTitle.isBlank()) ? titleFromPath(aemPath) : pageTitle.trim();
        List<Block> blocks = parseMarkdownBlocks(markdown);
        StringBuilder section = new StringBuilder();
        section.append("<h1>").append(esc(title)).append("</h1>");
        if (blocks.isEmpty()) {
            String fallback = defaultContent(markdown);
            if (!fallback.isBlank()) {
                section.append("<p>").append(esc(fallback)).append("</p>");
            }
        } else {
            for (Block b : blocks) {
                section.append(tableForm(b.name, b.cells));
            }
        }
        section.append(metadataTable(title, aemPath));
        return wrapBody(section.toString());
    }

    /** Inner &lt;main&gt; payload — what authors paste into the DA editor. */
    public static String pastePayload(String documentHtml) {
        if (documentHtml == null) {
            return "";
        }
        int mainOpen = documentHtml.toLowerCase(Locale.ROOT).indexOf("<main>");
        int mainClose = documentHtml.toLowerCase(Locale.ROOT).indexOf("</main>");
        if (mainOpen >= 0 && mainClose > mainOpen) {
            return documentHtml.substring(mainOpen + 6, mainClose).trim();
        }
        return documentHtml.trim();
    }

    static String wrapBody(String sectionInner) {
        return "<body>\n<header></header>\n<main>\n<div>\n"
                + sectionInner
                + "\n</div>\n</main>\n<footer></footer>\n</body>\n";
    }

    static String tableForm(String blockName, List<List<String>> rows) {
        String header = displayName(blockName);
        int width = 1;
        for (List<String> row : rows) {
            width = Math.max(width, Math.max(1, row.size()));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n<tr><td colspan=\"").append(width).append("\">").append(esc(header)).append("</td></tr>\n");
        if (rows.isEmpty()) {
            sb.append("<tr><td></td></tr>\n");
        } else {
            for (List<String> row : rows) {
                sb.append("<tr>");
                List<String> cells = new ArrayList<>(row);
                while (cells.size() < width) {
                    cells.add("");
                }
                for (String cell : cells) {
                    sb.append("<td>").append(cellHtml(cell)).append("</td>");
                }
                sb.append("</tr>\n");
            }
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    static String metadataTable(String title, String aemPath) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("title", title == null ? "" : title));
        if (aemPath != null && !aemPath.isBlank()) {
            rows.add(List.of("source-path", aemPath));
        }
        return tableForm("Metadata", rows);
    }

    private static String cellHtml(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String v = raw.trim();
        if (v.startsWith("<") && v.endsWith(">")) {
            return v;
        }
        if (looksLikeImage(v)) {
            return "<img src=\"" + esc(v) + "\" alt=\"\">";
        }
        if (v.contains("\n")) {
            StringBuilder p = new StringBuilder();
            for (String line : v.split("\n")) {
                if (!line.isBlank()) {
                    p.append("<p>").append(esc(line.trim())).append("</p>");
                }
            }
            return p.toString();
        }
        return esc(v);
    }

    private static boolean looksLikeImage(String v) {
        String lower = v.toLowerCase(Locale.ROOT);
        return lower.startsWith("/content/dam/")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg");
    }

    private static List<Block> parseMarkdownBlocks(String markdown) {
        List<Block> blocks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return blocks;
        }
        Block current = null;
        List<String> headers = new ArrayList<>();
        boolean headerConsumed = false;
        for (String raw : markdown.split("\n", -1)) {
            String line = raw.trim();
            if (line.startsWith("### ")) {
                current = new Block(line.substring(4).trim());
                blocks.add(current);
                headers = new ArrayList<>();
                headerConsumed = false;
                continue;
            }
            if (current == null || !line.startsWith("|")) {
                continue;
            }
            List<String> cols = splitRow(line);
            if (cols.isEmpty()) {
                continue;
            }
            if (cols.stream().allMatch(c -> c.replace("-", "").replace(":", "").isBlank())) {
                continue;
            }
            if (!headerConsumed) {
                boolean looksLikePropNames = cols.stream().anyMatch(DaDocumentBuilder::looksLikePropName);
                if (looksLikePropNames) {
                    headers = cols;
                    headerConsumed = true;
                    continue;
                }
                current.cells.add(cols);
                headerConsumed = true;
                continue;
            }
            if (!headers.isEmpty() && cols.size() == headers.size()) {
                for (int i = 0; i < cols.size(); i++) {
                    current.cells.add(List.of(headers.get(i), cols.get(i)));
                }
            } else {
                current.cells.add(cols);
            }
        }
        return blocks;
    }

    private static boolean looksLikePropName(String c) {
        String s = c.toLowerCase(Locale.ROOT);
        return s.startsWith("jcr:") || s.startsWith("file") || s.equals("image") || s.equals("heading")
                || s.equals("text") || s.equals("title") || s.contains(":");
    }

    private static List<String> splitRow(String line) {
        String[] parts = line.split("\\|", -1);
        List<String> cols = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (i == 0 || i == parts.length - 1) {
                continue;
            }
            cols.add(parts[i].trim().replace("\\|", "|"));
        }
        return cols;
    }

    private static String defaultContent(String markdown) {
        if (markdown == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String raw : markdown.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("|") || line.startsWith("---")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "Text";
        }
        if ("metadata".equalsIgnoreCase(name) || "meta-data".equalsIgnoreCase(name)) {
            return "Metadata";
        }
        String[] parts = name.replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private static String titleFromPath(String aemPath) {
        if (aemPath == null || aemPath.isBlank()) {
            return "Page";
        }
        String[] segs = aemPath.split("/");
        for (int i = segs.length - 1; i >= 0; i--) {
            if (!segs[i].isBlank()) {
                return displayName(segs[i]);
            }
        }
        return "Page";
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Used when building from live JCR properties (key/value rows). */
    public static String fromProperties(String pageTitle, String aemPath, List<Map<String, String>> namedPropMaps) {
        String title = (pageTitle == null || pageTitle.isBlank()) ? titleFromPath(aemPath) : pageTitle.trim();
        StringBuilder section = new StringBuilder();
        section.append("<h1>").append(esc(title)).append("</h1>");
        if (namedPropMaps != null) {
            for (Map<String, String> item : namedPropMaps) {
                String block = item.getOrDefault("_block", "Text");
                Map<String, String> props = new LinkedHashMap<>(item);
                props.remove("_block");
                List<List<String>> rows = new ArrayList<>();
                for (Map.Entry<String, String> e : props.entrySet()) {
                    rows.add(List.of(e.getKey(), e.getValue() == null ? "" : e.getValue()));
                }
                section.append(tableForm(block, rows));
            }
        }
        section.append(metadataTable(title, aemPath));
        return wrapBody(section.toString());
    }

    private static final class Block {
        final String name;
        final List<List<String>> cells = new ArrayList<>();

        Block(String name) {
            this.name = name;
        }
    }
}