# Block Generation

The `BlockGenerationAgent` produces one folder per distinct
target block. Each block is a self-contained JS decoration
function plus a CSS file.

## Folder structure

```
blocks/{name}/
├── {name}.js
└── {name}.css
```

Example: `blocks/cards/`

```js
// cards.js
export default function decorate(block) {
  const cards = block.querySelectorAll(':scope > div');
  cards.forEach((card) => {
    const picture = card.querySelector('picture');
    if (picture) {
      const wrapper = document.createElement('div');
      wrapper.className = 'cards-card-image';
      picture.parentNode.insertBefore(wrapper, picture);
      wrapper.appendChild(picture);
    }
  });
}
```

```css
/* cards.css */
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}
.cards-card-image {
  aspect-ratio: 16 / 9;
  overflow: hidden;
}
```

## Naming

Block names are kebab-case lowercase: `cards`, `hero`,
`accordion`. The `BlockGenerationAgent` derives the name
from the `ComponentMapping.targetBlock` field, which is
either the AI's suggestion or a deterministic mapping for
mock mode.

## JS decoration pattern

Every block exports a `decorate(block)` default function.
The block element is a `<div>` with one or more child
`<div>`s (one per row in the section model). The function
mutates the DOM in place to add classes, attributes, and
event listeners.

The modernizer does not generate `loadScript` / `loadCSS`
calls; EDS's loader handles that.

## CSS scoping

Every selector in `{name}.css` is prefixed with `.{name}` to
scope the styles. The modernizer's CSS linter enforces this.

## Performance budget

The modernizer keeps each block under 5 KB (uncompressed JS
+ CSS). Larger blocks trigger a `MEDIUM` issue; the
operator can review them in the dashboard.

## Customisation

Operators can post-process the generated blocks:

1. Review them in the dashboard's `#/diff` view.
2. Push a commit to the modernizer branch (or open a PR
   from the modernizer branch) that refactors the blocks.
3. The validation agents re-run; the report includes the
   new block sizes.

## Related

- [REPO_CONVENTIONS.md](REPO_CONVENTIONS.md) — the full
  repo layout.
- [../agents/BlockGenerationAgent.md](../agents/BlockGenerationAgent.md) —
  the agent that produces the blocks.
