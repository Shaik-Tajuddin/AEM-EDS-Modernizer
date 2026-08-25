# Product Hero — Test Block

> **This is a standalone test block** based on a Samsung Galaxy S26 promotional banner.  
> It is **not** part of the main reference documentation or block library.

## Design Reference

Based on `/home/ubuntu/Uploads/image.png` — a Samsung Galaxy S26 banner featuring:
- Two purple/violet phones on the left
- "Galaxy S26 | S26+" heading and "Galaxy AI ✨" subheading on the right
- "Learn more" text link + "Buy" outlined pill button
- Purple/violet gradient background

## File Structure

```
product_hero_test/
├── product-hero.js              # Block JavaScript (extractConfig → buildBlock → appendEvents)
├── product-hero.css             # Basic CSS styling (no SCSS, no variables)
├── _product-hero.json           # AEM Universal Editor component model
├── product-hero-example.html    # AEM-generated HTML example
├── demo.html                    # Standalone demo with inline mocks
└── README.md                    # This file
```

## Fields (Position-Based Extraction)

| Row | Field             | Component     | Description                        |
|-----|-------------------|---------------|------------------------------------|               
| 0   | image             | reference     | Product hero image                 |
| 1   | heading           | richtext      | Main title (e.g. "Galaxy S26")     |
| 2   | subheading        | richtext      | Tagline (e.g. "Galaxy AI ✨")      |
| 3   | primaryCtaText    | text          | Primary button label               |
| 4   | primaryCtaUrl     | aem-content   | Primary button link                |
| 5   | secondaryCtaText  | text          | Secondary link label               |
| 6   | secondaryCtaUrl   | aem-content   | Secondary link URL                 |

## Patterns Used

- **JS**: `extractConfig() → buildProductHero() → appendEvents()`
- **CSS**: Basic CSS with direct values, mobile-first responsive
- **JSON**: Underscore prefix `_product-hero.json`, tabs (General only)
- **Helpers**: `getImageFromRow`, `getHtmlFromRow`, `getTextFromRow`, `getLinkFromRow`

## Running the Demo

Open `demo.html` in a browser. It includes inline mocks for helper functions so it works without the full AEM EDS project setup.
