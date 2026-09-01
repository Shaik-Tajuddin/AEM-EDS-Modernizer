# Navigation Block (`navigation`)

## 1. Purpose
Renders the Navigation block with responsive layout and Universal Editor authoring.
Derived from AEM Component: `wknd/components/navigation`.

## 2. JCR Reference Content Path
- **Content Root:** `/content/wknd`
- **Sample Page Path:** `N/A`

## 3. For another AI / LLM
- **Pick this block when:** The AEM component is `wknd/components/navigation`.
- **Do not pick when:** A simple text paragraph suffices.

## 4. Fields / options
| Field | Component | Row? | Description |
|---|---|---|---|
| `id` | text | Yes (row 0) | Authorable unique block ID (page anchor & AI target) |
| `classes` | multiselect | No | Authorable CSS variant classes (dark-tone, compact, ...) |
| `title` | richtext | Yes (row 1) | JCR property `title` |
| `text` | richtext | Yes (row 2) | JCR property `text` |
| `ctaLink` | aem-content | Yes (row 3) | JCR property `ctaLink` |
| `ctaContent` | text | Yes (row 4) | JCR property `ctaContent` |

## 5. Row Map
- **Row 0:** `id`
- **Row 1:** `title`
- **Row 2:** `text`
- **Row 3:** `ctaLink`
- **Row 4:** `ctaContent`
