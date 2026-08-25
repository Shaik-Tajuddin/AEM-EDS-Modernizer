// Simple CTA Block Implementation
// Updated to match production text-callout patterns
//
// Import paths assume: blocks/simple-cta/simple-cta.js
// Adjust relative paths based on your project structure.

import {
  getTextFromRow,
  getHtmlFromRow,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Gets an anchor element from a row.
 * @param {Element|null} row
 * @returns {HTMLAnchorElement|null}
 */
function getAnchorFromRow(row) {
  const el = row?.querySelector('a');
  return el instanceof HTMLAnchorElement ? el : null;
}

/**
 * @param {HTMLAnchorElement|null} anchor
 * @returns {boolean}
 */
function hasUsableHref(anchor) {
  if (!anchor) return false;
  const href = anchor.getAttribute('href');
  return Boolean(href && href !== '#');
}

/**
 * Row layout: tabs do not create rows. Fields whose name starts with `classes`
 * are applied to the block by the runtime and do not create rows.
 *
 * 0: id
 * 1: title (richtext)
 * 2: text (richtext)
 * 3: ctaLink (aem-content)
 * 4: ctaContent (richtext)
 *
 * JS flow: extractConfig() → buildSimpleCta() → appendEvents()
 *
 * @param {Element} block
 * @returns {Object}
 */
function extractConfig(block) {
  if (!block) return {};

  const rows = [...block.children];

  return {
    id: getTextFromRow(rows[0]),
    title: getHtmlFromRow(rows[1]),
    text: getHtmlFromRow(rows[2]),
    ctaLink: getAnchorFromRow(rows[3]),
    ctaContent: getHtmlFromRow(rows[4]),
  };
}

/**
 * @param {Object} config
 * @param {HTMLElement} [config.mainEl]
 */
function appendEvents(config) {
  if (!config.mainEl) return;
  config.mainEl.addEventListener('click', () => {
    /* eslint-disable no-console -- intentional debug hook */
    console.log('simple-cta: CTA clicked');
    /* eslint-enable no-console */
  });
}

/**
 * Build CTA element — reuse anchor from AEM or create button fallback.
 *
 * @param {Object} config
 * @returns {HTMLElement}
 */
function buildCtaElement(config) {
  const { ctaLink, ctaContent } = config;
  const labelHtml = ctaContent?.trim()
    ? ctaContent
    : '<span>Learn more</span>';

  if (ctaLink && hasUsableHref(ctaLink)) {
    if (ctaContent?.trim()) {
      ctaLink.innerHTML = ctaContent;
    }
    ctaLink.classList.add('brand-cta');
    return ctaLink;
  }

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.classList.add('brand-cta', 'simple-cta-cta-button');
  btn.innerHTML = labelHtml;
  return btn;
}

/**
 * @param {Element} block
 * @param {Object} config
 */
function buildSimpleCta(block, config) {
  const {
    id,
    title,
    text,
  } = config;

  if (id) {
    block.id = id;
  }

  const inner = document.createElement('div');
  inner.classList.add('simple-cta-inner');

  if (title) {
    const titleEl = document.createElement('div');
    titleEl.classList.add('simple-cta-title');
    titleEl.innerHTML = title;
    inner.appendChild(titleEl);
  }

  if (text) {
    const textEl = document.createElement('div');
    textEl.classList.add('simple-cta-text');
    textEl.innerHTML = text;
    inner.appendChild(textEl);
  }

  const ctaEl = buildCtaElement(config);

  // Store mainEl for event binding
  config.mainEl = ctaEl;

  const wrap = document.createElement('div');
  wrap.classList.add('simple-cta-cta');
  wrap.appendChild(ctaEl);

  inner.appendChild(wrap);

  block.textContent = '';
  block.appendChild(inner);
}

/**
 * Block entry point.
 *
 * JS flow: extractConfig() → buildSimpleCta() → appendEvents()
 *
 * @param {Element} block
 */
export default function decorate(block) {
  const config = extractConfig(block);
  buildSimpleCta(block, config);
  appendEvents(config);
}
