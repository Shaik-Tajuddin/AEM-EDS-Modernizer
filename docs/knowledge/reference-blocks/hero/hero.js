/**
 * Hero Block
 *
 * Reference implementation — follows all Phase 6b standards:
 * - classList.add() (no className =)
 * - config.mainEl set in buildBlock
 * - No analytics
 * - Position-based row extraction via [...block.children]
 * - Synchronous decorate()
 * - Hyphenated class names (no BEM)
 *
 * Row layout (from _hero.json):
 *   Row 0: id (text)
 *   Row 1: eyebrow (richtext)
 *   Row 2: headline (text)
 *   Row 3: description (textarea)
 *   Row 4: disclaimer (textarea)
 *   Row 5: image (reference)
 *   Row 6: imageAlt (text)
 */

import {
  getTextFromRow,
  getHtmlFromRow,
  getImageFromRow,
} from '../../scripts/utilities/block-helpers.js';

// ============================================================================
// CONFIGURATION EXTRACTION
// ============================================================================

/**
 * Extract block configuration from DOM structure.
 * Uses position-based row extraction via [...block.children].
 *
 * @param {HTMLElement} block The hero block element
 * @returns {Object} Extracted configuration object
 */
function extractConfig(block) {
  const rows = [...block.children];

  return {
    id: getTextFromRow(rows[0]) || '',
    eyebrow: getHtmlFromRow(rows[1]) || '',
    headline: getTextFromRow(rows[2]) || '',
    description: getTextFromRow(rows[3]) || '',
    disclaimer: getTextFromRow(rows[4]) || '',
    image: getImageFromRow(rows[5]) || '',
    imageAlt: getTextFromRow(rows[6]) || '',
  };
}

// ============================================================================
// BLOCK BUILDING
// ============================================================================

/**
 * Build the hero block DOM structure from configuration.
 * Uses classList.add() for all class assignments.
 * Sets config.mainEl to the content area.
 *
 * @param {HTMLElement} block The hero block element
 * @param {Object} config The configuration object
 */
function buildHero(block, config) {
  const {
    id,
    eyebrow,
    headline,
    description,
    disclaimer,
    image,
    imageAlt,
  } = config;

  if (id) {
    block.id = id;
  }

  // Build content area
  const contentArea = document.createElement('div');
  contentArea.classList.add('hero-content');

  if (eyebrow) {
    const eyebrowEl = document.createElement('div');
    eyebrowEl.classList.add('hero-eyebrow');
    eyebrowEl.innerHTML = eyebrow;
    contentArea.appendChild(eyebrowEl);
  }

  if (headline) {
    const headlineEl = document.createElement('h1');
    headlineEl.classList.add('hero-headline');
    headlineEl.textContent = headline;
    contentArea.appendChild(headlineEl);
  }

  if (description) {
    const descriptionEl = document.createElement('p');
    descriptionEl.classList.add('hero-description');
    descriptionEl.textContent = description;
    contentArea.appendChild(descriptionEl);
  }

  if (disclaimer) {
    const disclaimerEl = document.createElement('p');
    disclaimerEl.classList.add('hero-disclaimer');
    disclaimerEl.textContent = disclaimer;
    contentArea.appendChild(disclaimerEl);
  }

  // Build image area
  const imageWrapper = document.createElement('div');
  imageWrapper.classList.add('hero-image-wrapper');

  if (image) {
    const imgEl = document.createElement('img');
    imgEl.src = typeof image === 'string' ? image : image.src || '';
    imgEl.alt = imageAlt;
    imgEl.classList.add('hero-image');
    imgEl.loading = 'eager'; // Hero image is LCP candidate
    imageWrapper.appendChild(imgEl);
  }

  // Set mainEl BEFORE clearing block
  config.mainEl = contentArea;

  // Assemble
  block.textContent = '';
  block.appendChild(contentArea);
  block.appendChild(imageWrapper);
}

// ============================================================================
// EVENT LISTENERS
// ============================================================================

/**
 * Append event listeners (no analytics in this project).
 *
 * @param {Object} config The configuration object
 */
function appendEvents(config) {
  if (!config.mainEl) return;
  // No analytics — empty shell per project standards
}

// ============================================================================
// MAIN BLOCK DECORATOR
// ============================================================================

/**
 * Decorate the hero block.
 * JS flow: extractConfig() → buildHero() → appendEvents()
 *
 * @param {HTMLElement} block The hero block element
 */
export default function decorate(block) {
  const config = extractConfig(block);
  buildHero(block, config);
  appendEvents(config);
}
