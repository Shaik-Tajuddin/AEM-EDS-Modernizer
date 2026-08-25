# AEM EDS: A Developer's Guide to Component Tabs and JSON Merging

**Report Date: 2026-04-03**

## Introduction

This guide provides practical, in-depth documentation for developers working with Adobe Experience Manager (AEM) Edge Delivery Services (EDS). The primary objective is to equip developers with the knowledge to effectively structure component models for the Universal Editor and to leverage powerful command-line tools for managing JSON configurations.

The report is divided into two main sections. The first section details the correct JSON structure for implementing a tabbed interface (e.g., General, Appearance, Analytics) within AEM EDS component models, a common requirement for organizing complex authoring dialogues. The second section provides a comprehensive explanation of `json-merge-cli`, a utility that uses a spread operator (`...`) for intelligently combining JSON files. This section covers its core functionality, common merge patterns, and practical applications within an AEM EDS project, drawing on real-world patterns from AEM.live documentation and xwalk implementations.

## Implementing Tabs in AEM EDS Component Models

In AEM EDS, the authoring experience for components in the Universal Editor is defined by a set of JSON configuration files, primarily `component-models.json` and `component-definitions.json`. A well-organized properties panel is crucial for author efficiency, and tabs are the standard mechanism for grouping related fields.

### The Role of `component-models.json`

The `component-models.json` file is the blueprint for a component's data structure. It defines the fields that an author can interact with in the properties panel. Each entry in this file corresponds to a component model, identified by a unique `id`, and contains a `fields` array that lists the authorable properties. These fields are ultimately persisted as properties in AEM.

### Using the `tab` Component Type for UI Organization

The Universal Editor provides a specific field type, `"component": "tab"`, designed to group other input fields into a tabbed interface. This is not a data-storing field itself but a structural component that enhances the layout of the properties panel, making complex components with numerous options more manageable for authors.

To implement tabs, you define a field of type `tab`. This tab field then contains its own `fields` array, where each object represents a single tab panel. These panels are typically defined using a `"component": "container"` and are given a `label` that appears as the tab's title. The actual input fields for that tab are then nested within the container's `fields` array.

### Example: Creating General, Appearance, and Analytics Tabs

The following is a complete, practical example of a `component-models.json` entry for a "Teaser" component. It demonstrates how to structure the JSON to create three distinct tabs: General, Appearance, and Analytics.

```json
{
  "id": "teaser",
  "fields": [
    {
      "component": "tab",
      "name": "tabs",
      "fields": [
        {
          "component": "container",
          "label": "General",
          "fields": [
            {
              "component": "reference",
              "name": "image",
              "label": "Image",
              "description": "Select the primary image for the teaser."
            },
            {
              "component": "text",
              "name": "title",
              "label": "Title",
              "valueType": "string",
              "description": "Enter the main headline."
            },
            {
              "component": "richtext",
              "name": "description",
              "label": "Description",
              "valueType": "string",
              "description": "Enter the descriptive text."
            },
            {
              "component": "reference",
              "name": "ctaLink",
              "label": "CTA Link",
              "description": "Select the link for the call-to-action button."
            },
            {
              "component": "text",
              "name": "ctaText",
              "label": "CTA Text",
              "valueType": "string",
              "description": "Enter the text for the call-to-action button."
            }
          ]
        },
        {
          "component": "container",
          "label": "Appearance",
          "fields": [
            {
              "component": "select",
              "name": "style",
              "label": "Style Variant",
              "valueType": "string",
              "description": "Select the visual style for the teaser.",
              "options": [
                { "name": "Default", "value": "default" },
                { "name": "Inverted", "value": "inverted" },
                { "name": "Card", "value": "card" }
              ]
            }
          ]
        },
        {
          "component": "container",
          "label": "Analytics",
          "fields": [
            {
              "component": "text",
              "name": "data-cmp-id",
              "label": "Component ID",
              "valueType": "string",
              "description": "Unique ID for tracking this component instance."
            }
          ]
        }
      ]
    }
  ]
}
```

### Data Layer Integration for the Analytics Tab

The AEM Core Components provide a robust, out-of-the-box integration with the Adobe Client Data Layer (ACDL). This integration can be enabled for custom components to facilitate analytics and tracking.

To make a component's data available to the data layer, you must add the `data-cmp-data-layer` attribute to its wrapping HTML element in the component's script (e.g., `component.js` or HTL). The value of this attribute should be a JSON string of the data you wish to expose.

The fields defined in the "Analytics" tab of the component model can be used to populate this data. For example, the `data-cmp-id` field from the example above can be used to set a unique tracking identifier on the component.

When a user interacts with a component, specific events are triggered in the data layer:
*   **`cmp:show` / `cmp:hide`**: Fired when components like Tabs, Accordions, or Carousels show or hide a panel.
*   **`cmp:click`**: Fired when a user clicks on an element that has the `data-cmp-clickable` attribute.
*   **`cmp:loaded`**: Fired once the data layer has been populated with all the component data on the page.

By structuring your component model with an Analytics tab and ensuring your client-side scripts populate the `data-cmp-data-layer` attribute correctly, you can create a standardized and powerful analytics implementation.

## Mastering `json-merge-cli` for AEM EDS Development

In AEM EDS projects, especially those following the xwalk architecture, managing JSON configurations is a common task. Component definitions, models, and filters are often spread across multiple files to promote reusability and maintainability. The `merge-json-cli` utility is a lightweight, powerful tool for combining these JSON files.

### Introduction to `merge-json-cli`

`merge-json-cli` is a Node.js-based command-line tool that merges JSON files using semantics similar to JavaScript's spread syntax (`...`). It allows developers to define a base JSON file and "spread" the contents of other JSON files into it, creating a single, consolidated output file.

The tool can be run directly using `npx` without a local installation:
```bash
npx https://github.com/Buuhuu/merge-json-cli --in ./input.json --out ./output.json
```

### The Spread Operator (`...`) in Action

The core feature of `merge-json-cli` is its use of a special key-value pair to indicate a merge operation. Within a JSON object, the syntax `"...": "./path/to/another-file.json"` instructs the tool to fetch the specified file and merge its contents at that location.

#### Object Merging
When the spread operator is used inside a JSON object, the keys and values from the referenced file are injected into the parent object. If there are conflicting keys, the values from the referenced (spread) file take precedence, overwriting the values in the base file.

**Example:**

`base.json`:
```json
{
  "title": "Base Title",
  "author": "Base Author",
  "...": "./override.json",
  "version": "1.0"
}
```

`override.json`:
```json
{
  "title": "Overridden Title",
  "status": "Published"
}
```

Running `npx merge-json-cli --in ./base.json --out ./merged.json` produces:

`merged.json`:
```json
{
  "title": "Overridden Title",
  "author": "Base Author",
  "status": "Published",
  "version": "1.0"
}
```
Notice that `title` was overwritten by the value from `override.json`, and the new `status` key was added.

#### Array Merging
The spread operator can also be used to merge content into arrays. If an object within a source array contains only the spread operator, and the referenced file contains an array, its items are spliced into the source array at that position. This is particularly useful for combining lists of configurations.

The tool also supports glob patterns, allowing you to merge multiple files at once.

**Example:**

`input.json`:
```json
[
  { "name": "Header" },
  { "...": "./components/*.json" },
  { "name": "Footer" }
]
```
If a `components` directory contains `teaser.json` and `hero.json`, both containing JSON objects, the final merged array will contain the Header, the contents of `teaser.json` and `hero.json`, and the Footer.

### Practical Use Case: Merging Component Definitions

A common pattern in xwalk projects is to have a set of core, reusable component definitions and another set of project-specific definitions. `json-merge-cli` is ideal for combining these into the final `component-definitions.json` file used by the Universal Editor.

**Scenario:**
*   `core-definitions.json`: Contains definitions for standard components like Text and Image.
*   `project-definitions.json`: Imports the core definitions and adds a project-specific "Campaign Banner" component.

`core-definitions.json`:
```json
[
  {
    "title": "Text",
    "id": "text",
    "plugins": { /* ... */ }
  },
  {
    "title": "Image",
    "id": "image",
    "plugins": { /* ... */ }
  }
]
```

`project-definitions.json`:
```json
[
  { "...": "./core-definitions.json" },
  {
    "title": "Campaign Banner",
    "id": "campaign-banner",
    "plugins": {
      "xwalk": {
        "page": {
          "resourceType": "core/franklin/components/block/v1/block",
          "template": {
            "name": "Campaign Banner",
            "model": "campaign-banner"
          }
        }
      }
    }
  }
]
```

The command `npx merge-json-cli --in ./project-definitions.json --out ./component-definitions.json` would generate a single `component-definitions.json` file containing the Text, Image, and Campaign Banner definitions, ready for use by the editor.

## Advanced Merging Concepts and Alternatives

While `merge-json-cli` is excellent for straightforward merging, complex projects may require more advanced capabilities, especially when dealing with arrays of objects.

### Handling Complex Array Merges

A common challenge is merging two arrays of objects where you want to update an existing object based on a unique identifier (e.g., an `id` field) rather than simply concatenating the arrays. The simple `merge-json-cli` does not directly support this "merge-by-id" logic.

For these scenarios, other tools provide more granular control:
*   **`jsonmerge` (Java-based):** This tool allows you to specify a "distinct key" for array merging using JSONPath expressions. For example, you can instruct it to merge objects within an array where the `name` property matches.
*   **`json-merger` (Node.js-based):** This library offers a `$match` operator, which can find an item in an array by index or a query and merge new data into it.

### Alternative Merge Tools

For developers facing requirements beyond the scope of `merge-json-cli`, the following tools are worth considering:

*   **`json-merger`:** This Node.js package provides a rich set of explicit operators (prefixed with `$`) for fine-grained control over the merge process. Key operators include:
    *   `$import`: Imports other files.
    *   `$merge`: Recursively merges objects.
    *   `$replace`: Replaces a value entirely.
    *   `$remove`: Deletes a key or an array item.
    *   `$concat`, `$append`, `$prepend`: Provide specific array manipulation operations.

*   **`jq`:** A powerful command-line JSON processor. While not strictly a merge tool, `jq` can perform complex transformations and merges through its query language. The `*` operator can perform a recursive merge, and its `reduce` function can be used to iterate over and combine multiple JSON documents. `jq` is highly effective for scripting complex data pipelines.

## Conclusion

A well-structured authoring experience is fundamental to the success of any AEM project. By leveraging the `tab` component type in `component-models.json`, developers can create clean, intuitive, and scalable properties panels in the Universal Editor. This organization simplifies content authoring for complex components by logically grouping fields for content, appearance, and analytics.

Furthermore, effective configuration management is critical for maintaining a clean and reusable codebase. The `json-merge-cli` tool, with its simple yet powerful spread operator (`...`), provides an elegant solution for combining JSON files. This approach is perfectly suited for AEM EDS and xwalk projects, enabling developers to manage shared component libraries and project-specific overrides with ease. For more complex merge requirements, alternative tools like `json-merger` and `jq` offer advanced capabilities to handle any data integration scenario. By mastering these techniques, developers can build more robust, maintainable, and user-friendly AEM Edge Delivery Services solutions.

# References
1. [AEM Edge Delivery Services - Universal Editor Tutorial - aem.live](https://www.aem.live/developer/ue-tutorial)
2. [AEM Edge Delivery Services - Component Model Definitions - aem.live](https://www.aem.live/developer/component-model-definitions)
3. [AEM Edge Delivery Services - Universal Editor Blocks - aem.live](https://www.aem.live/developer/universal-editor-blocks)
4. [AEM Edge Delivery Services - Spreadsheets - aem.live](https://www.aem.live/developer/spreadsheets)
5. [AEM Edge Delivery Services - Authoring Path Mapping - aem.live](https://www.aem.live/developer/authoring-path-mapping)
6. [AEM Edge Delivery Services - Content Fragment Overlay - aem.live](https://www.aem.live/developer/content-fragment-overlay)
7. [AEM Edge Delivery Services - Authoring Tabular Data - aem.live](https://www.aem.live/docs/authoring-tabular-data)
8. [AEM Edge Delivery Services - Lifecycle - aem.live](https://www.aem.live/docs/lifecycle)
9. [AEM Edge Delivery Services - FAQ - aem.live](https://www.aem.live/docs/faq)
10. [AEM Edge Delivery Services - Schema for Structured Data - aem.live](https://www.aem.live/docs/schema-structured-data)
11. [Universal Editor Field Types - Adobe Experience League](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/universal-editor/field-types)
12. [How to create brand specific repoless EDS sites in AEM Edge Delivery - Experience AEM](http://experience-aem.blogspot.com/2025/11/aem-edge-delivery-create-brand-specific-repoless-eds-sites.html)
13. [How to setup an AEM Edge Delivery Services project - Adobe Experience Cloud](https://experienceleague.adobe.com/en/docs/experience-cloud-kcs/kbarticles/ka-28055)
14. [Tabs - Adobe Experience Manager](https://experienceleague.adobe.com/docs/experience-manager-core-components/using/components/tabs.html?lang=en)
15. [Metadata Schemas - Adobe Experience League](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/assets/manage/metadata-schemas)
16. [Content Fragment Models - Adobe Experience Manager 6.5](https://experienceleague.adobe.com/en/docs/experience-manager-65/content/assets/content-fragments/content-fragments-models)
17. [Querying Submitted Form Data - Adobe Experience League](https://experienceleague.adobe.com/docs/experience-manager-learn/forms/querying-submitted-data/introduction.html?lang=en)
18. [Universal Editor with Edge Delivery Service for AEM - AEM Corner](https://aemcorner.com/universal-editor-with-edge-delivery-service-for-aem/)
19. [Introducing the Universal Editor for Adobe Experience Manager - Boye & Co](https://www.boye-co.com/blog/2024/4/introducing-the-universal-editor-for-adobe-experience-manager)
20. [Component Models, Filters & Definitions for Universal Editor with EDS - Medium](https://medium.com/@mayursatav/component-models-filters-definitions-for-universal-editor-with-eds-29f54a61ecfb)
21. [How to build custom AEM metadata schemas with JSON based dropdowns - Medium](https://medium.com/@arindamc65.ac/how-to-build-custom-aem-metadata-schemas-with-json-based-dropdowns-a93780a27ad2)
22. [Data Layer Integration - GitHub](https://github.com/adobe/aem-core-wcm-components/blob/main/DATA_LAYER_INTEGRATION.md)
23. [aem-component-generator - GitHub](https://github.com/adobe/aem-component-generator)
24. [aem-boilerplate-xwalk - GitHub](https://github.com/adobe-rnd/aem-boilerplate-xwalk)
25. [aem-boilerplate-xwalk Releases - GitHub](https://github.com/adobe-rnd/aem-boilerplate-xwalk/releases)
26. [aem-boilerplate-xcom - GitHub](https://github.com/adobe-rnd/aem-boilerplate-xcom)
27. [xwalk-boilerplate - GitHub](https://github.com/diva-e-aem/xwalk-boilerplate)
28. [merge-json-cli - npm](https://www.npmjs.com/package/merge-json-cli)
29. [json-merger - npm](https://www.npmjs.com/package/json-merger)
30. [npm: merge-json-cli - Socket](https://socket.dev/npm/package/merge-json-cli)
31. [jsonmerge - GitHub](https://github.com/oswaldobapvicjr/jsonmerge)
32. [json-merge-cli - GitHub](https://github.com/tomas-sereikis/json-merge-cli)
33. [json-merger - GitHub](https://github.com/boschni/json-merger)
34. [JSON.MERGE - Redis](https://redis.io/docs/latest/commands/json.merge/)
35. [How to merge 2 json objects from 2 files using jq - Stack Overflow](https://stackoverflow.com/questions/19529688/how-to-merge-2-json-objects-from-2-files-using-jq)
36. [JavaScript Three Dots (...) Spread Operator - Fjolt](https://fjolt.com/article/javascript-three-dots-spread-operator)
37. [How to Merge JSON Files in Linux - Baeldung](https://www.baeldung.com/linux/json-merge-files)
38. [Three Dots “...” in JavaScript - Dev Community](https://dev.to/sagar/three-dots---in-javascript-26ci)
39. [jq: how do I merge and add multiple json objects into one json object? - Reddit](https://www.reddit.com/r/commandline/comments/10w2mj8/jq_how_do_i_merge_and_add_multiple_json_objects/)
40. [jsonmerge - PyPI](https://pypi.org/project/jsonmerge/)
41. [Merge/concat multiple JSONObjects in Java - Stack Overflow](https://stackoverflow.com/questions/2403132/merge-concat-multiple-jsonobjects-in-java)
42. [Merge of two complicated json objects - Stack Overflow](https://stackoverflow.com/questions/55538264/merge-of-two-complicated-json-objects)
43. [How to merge two json documents in json.net - Stack Overflow](https://stackoverflow.com/questions/68019344/how-to-merge-two-json-documents-in-json-net)
44. [How to merge multiple json object into one json array in shell - Unix & Linux Stack Exchange](https://unix.stackexchange.com/questions/726238/how-to-merge-multiple-json-object-into-one-json-array-in-shell)