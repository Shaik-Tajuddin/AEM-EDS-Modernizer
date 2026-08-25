# Block Helpers — Practical Usage Examples

This document shows real-world examples of using the block helpers in AEM EDS block development.

---

## 1. Basic Value Extraction

### Simple Block with extractConfig

The most common use case — extracting authored values from AEM's `div > div` structure.

```js
import {
  getText, getHTML, getLink, getImage,
} from '../../utilities/block-helpers.js';

/**
 * Position map (matches JSON model field order):
 *   0 = id (plain text)
 *   1 = title (richtext)
 *   2 = description (richtext)
 *   3 = image (reference)
 *   4 = ctaUrl (aem-content)
 */
function extractConfig(block) {
  return {
    id: getText(block, 0),
    title: getHTML(block, 1),
    description: getHTML(block, 2),
    image: getImage(block, 3),
    cta: getLink(block, 4),
  };
}
```

### Handling Optional Fields with Defaults

```js
import { getText, getHTML, getLink } from '../../utilities/block-helpers.js';

function extractConfig(block) {
  return {
    id: getText(block, 0) || 'default-id',
    title: getHTML(block, 1) || '<h2>Default Title</h2>',
    subtitle: getHTML(block, 2),  // empty string if not authored
    cta: getLink(block, 3),       // { href: '', text: '', element: null } if empty
  };
}
```

---

## 2. Responsive Design Patterns

### Different Images for Mobile/Desktop

```js
import {
  getImage, createResponsiveHelper,
} from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  const mobileImg = getImage(block, 3);
  const desktopImg = getImage(block, 4);
  const responsive = createResponsiveHelper();

  block.innerHTML = '';

  const imgEl = document.createElement('img');
  imgEl.className = 'hero__image';

  function updateImage() {
    const img = responsive.isDesktop() ? desktopImg : mobileImg;
    if (img) {
      imgEl.src = img.src;
      imgEl.alt = img.alt;
    }
  }

  updateImage();
  responsive.onBreakpointChange(updateImage);
  block.appendChild(imgEl);
}
```

### Desktop-Only Layout Toggle

```js
import { createDesktopHelper } from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  // ... build block ...

  // Toggle a CSS class based on breakpoint
  createDesktopHelper((isDesktop) => {
    block.classList.toggle('card-grid--horizontal', isDesktop);
    block.classList.toggle('card-grid--stacked', !isDesktop);
  });
}
```

### Custom Breakpoint

```js
import { createResponsiveHelper } from '../../utilities/block-helpers.js';

// Use 1200px breakpoint for wide layouts
const wideScreen = createResponsiveHelper(1200);
if (wideScreen.isDesktop()) {
  // 3-column layout
} else {
  // 2-column or stacked layout
}
```

---

## 3. Block Grouping — Accordion

An accordion where each `<h2>` in a row starts a new collapsible section.

```js
import {
  createAdvancedBlockGrouper,
  getHtmlFromRow,
  createToggle,
} from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  // Group rows: each group starts with a row containing an <h2>
  const groups = createAdvancedBlockGrouper(block, {
    isSeparator: (row) => !!row.querySelector('h2'),
  });

  block.innerHTML = '';
  block.classList.add('accordion');

  groups.forEach((groupRows) => {
    // First row is the header
    const headerHtml = getHtmlFromRow(groupRows[0]);

    // Remaining rows are the content
    const contentHtml = groupRows
      .slice(1)
      .map((row) => getHtmlFromRow(row))
      .join('');

    const toggle = createToggle({
      trigger: headerHtml,
      content: contentHtml,
      className: 'accordion',
    });

    block.appendChild(toggle.wrapper);
  });

  block.classList.add('accordion--ready');
}
```

---

## 4. Block Grouping — Carousel

A carousel where each item uses 3 consecutive rows (image, title, text).

```js
import {
  createBlockGrouper,
  getImageFromRow,
  getTextFromRow,
  getHtmlFromRow,
} from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  // Each carousel slide = 3 rows
  const slides = createBlockGrouper(block, 3);

  block.innerHTML = '';
  block.classList.add('carousel');

  const track = document.createElement('div');
  track.className = 'carousel__track';

  slides.forEach((slideRows, index) => {
    const image = getImageFromRow(slideRows[0]);
    const title = getTextFromRow(slideRows[1]);
    const description = getHtmlFromRow(slideRows[2]);

    const slide = document.createElement('div');
    slide.className = 'carousel__slide';
    slide.innerHTML = `
      ${image ? `<img src="${image.src}" alt="${image.alt}" class="carousel__image">` : ''}
      <h3 class="carousel__title">${title}</h3>
      <div class="carousel__description">${description}</div>
    `;

    track.appendChild(slide);
  });

  block.appendChild(track);
  block.classList.add('carousel--ready');
}
```

---

## 5. Block Grouping — Tabs

Tabs where odd rows are tab labels and even rows are tab content.

```js
import {
  createAdvancedBlockGrouper,
  getTextFromRow,
  getHtmlFromRow,
} from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  // Every 2 rows = one tab (label + content)
  const groups = createAdvancedBlockGrouper(block, {
    isSeparator: (row, index) => index % 2 === 0, // even rows are separators
  });

  block.innerHTML = '';

  const tabList = document.createElement('div');
  tabList.className = 'tabs__list';
  tabList.setAttribute('role', 'tablist');

  const tabPanels = document.createElement('div');
  tabPanels.className = 'tabs__panels';

  groups.forEach((groupRows, i) => {
    const label = getTextFromRow(groupRows[0]);
    const content = groupRows.length > 1 ? getHtmlFromRow(groupRows[1]) : '';
    const id = `tab-${i}`;

    // Tab button
    const tab = document.createElement('button');
    tab.className = 'tabs__tab';
    tab.setAttribute('role', 'tab');
    tab.setAttribute('aria-controls', `${id}-panel`);
    tab.setAttribute('aria-selected', i === 0 ? 'true' : 'false');
    tab.id = `${id}-tab`;
    tab.textContent = label;
    tabList.appendChild(tab);

    // Tab panel
    const panel = document.createElement('div');
    panel.className = 'tabs__panel';
    panel.setAttribute('role', 'tabpanel');
    panel.setAttribute('aria-labelledby', `${id}-tab`);
    panel.id = `${id}-panel`;
    panel.innerHTML = content;
    panel.style.display = i === 0 ? '' : 'none';
    tabPanels.appendChild(panel);
  });

  block.appendChild(tabList);
  block.appendChild(tabPanels);

  // Wire up tab switching
  tabList.querySelectorAll('.tabs__tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      // Deselect all
      tabList.querySelectorAll('.tabs__tab').forEach((t) => t.setAttribute('aria-selected', 'false'));
      tabPanels.querySelectorAll('.tabs__panel').forEach((p) => { p.style.display = 'none'; });
      // Select clicked
      tab.setAttribute('aria-selected', 'true');
      const panelId = tab.getAttribute('aria-controls');
      document.getElementById(panelId).style.display = '';
    });
  });

  block.classList.add('tabs--ready');
}
```

---

## 6. Toggle / Expand — FAQ Block

```js
import { getText, getHTML, createToggle } from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  const rows = block.querySelectorAll(':scope > div');
  block.innerHTML = '';
  block.classList.add('faq');

  // Each row pair = question + answer
  for (let i = 0; i < rows.length; i += 2) {
    const question = rows[i]?.querySelector('div')?.textContent?.trim() || '';
    const answer = rows[i + 1]?.querySelector('div')?.innerHTML?.trim() || '';

    if (question) {
      const toggle = createToggle({
        trigger: question,
        content: answer,
        className: 'faq',
      });
      block.appendChild(toggle.wrapper);
    }
  }

  block.classList.add('faq--ready');
}
```

---

## 7. Toggle / Expand — Using addToggleListeners on Existing DOM

```js
import { addToggleListeners } from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  // Assume block HTML already has trigger/panel structure from buildBlock()
  addToggleListeners(block, {
    triggerSelector: '.accordion__trigger',
    panelSelector: '.accordion__panel',
    activeClass: 'accordion__item--expanded',
    accordion: true, // only one open at a time
    onChange: (trigger, panel, expanded) => {
      // Optional: track analytics
      if (expanded && window.mitt) {
        window.mitt.emit('analytics:track', {
          event: 'accordion:expand',
          label: trigger.textContent,
        });
      }
    },
  });
}
```

---

## 8. Author Mode Detection

### Skip Animations in Author Mode

```js
import { isAuthorMode } from '../../utilities/block-helpers.js';

export default async function decorate(block) {
  const config = extractConfig(block);
  buildBlock(block, config);

  if (isAuthorMode()) {
    // In author mode: show everything immediately, no animations
    block.classList.add('block--author-preview');
    block.querySelectorAll('[style*="display: none"]').forEach((el) => {
      el.style.display = '';
    });
    return; // Skip event listeners / animations
  }

  // Normal publish mode
  appendEventListeners(block, config);
  block.classList.add('block--ready');
}
```

### Conditional Content in Author Mode

```js
import { isAuthorMode } from '../../utilities/block-helpers.js';

function buildBlock(block, config) {
  block.innerHTML = '';

  // Show placeholder message in author mode if no content
  if (isAuthorMode() && !config.title && !config.text) {
    const placeholder = document.createElement('div');
    placeholder.className = 'block__placeholder';
    placeholder.textContent = 'Add content using the properties panel →';
    block.appendChild(placeholder);
    return;
  }

  // Normal block building...
}
```

---

## 9. Combining Multiple Helpers

A complete block using several helpers together:

```js
import {
  getText, getHTML, getImage, getLink,
  createResponsiveHelper, isAuthorMode,
} from '../../utilities/block-helpers.js';

function extractConfig(block) {
  return {
    id: getText(block, 0),
    title: getHTML(block, 1),
    description: getHTML(block, 2),
    imageMobile: getImage(block, 3),
    imageDesktop: getImage(block, 4),
    cta: getLink(block, 5),
  };
}

function buildBlock(block, config) {
  block.innerHTML = '';

  if (config.id) block.setAttribute('data-block-id', config.id);

  const content = document.createElement('div');
  content.className = 'promo__content';

  if (config.title) {
    const title = document.createElement('div');
    title.className = 'promo__title';
    title.innerHTML = config.title;
    content.appendChild(title);
  }

  if (config.description) {
    const desc = document.createElement('div');
    desc.className = 'promo__description';
    desc.innerHTML = config.description;
    content.appendChild(desc);
  }

  if (config.cta.href) {
    const btn = document.createElement('a');
    btn.href = config.cta.href;
    btn.className = 'promo__cta';
    btn.textContent = config.cta.text || 'Learn More';
    content.appendChild(btn);
  }

  block.appendChild(content);
}

export default async function decorate(block) {
  if (isAuthorMode()) {
    block.classList.add('promo--author-preview');
  }

  const config = extractConfig(block);
  buildBlock(block, config);

  // Responsive image switching
  if (config.imageMobile || config.imageDesktop) {
    const responsive = createResponsiveHelper();
    const imgEl = document.createElement('img');
    imgEl.className = 'promo__image';

    function setImage() {
      const img = responsive.isDesktop()
        ? (config.imageDesktop || config.imageMobile)
        : (config.imageMobile || config.imageDesktop);
      if (img) {
        imgEl.src = img.src;
        imgEl.alt = img.alt;
      }
    }

    setImage();
    responsive.onBreakpointChange(setImage);
    block.appendChild(imgEl);
  }

  block.classList.add('promo--ready');
}
```
