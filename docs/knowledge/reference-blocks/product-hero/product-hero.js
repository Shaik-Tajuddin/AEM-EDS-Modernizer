import {
  getHtmlFromRow,
  getTextFromRow,
  getImageFromRow,
  getLinkFromRow,
} from '../../scripts/utilities/block-helpers.js';

/**
 * Row layout (tabs/classes fields do not create rows):
 *
 * 0: image (reference — product hero image)
 * 1: heading (richtext — e.g. "Galaxy S26 | S26+")
 * 2: subheading (richtext — e.g. "Galaxy AI ✨")
 * 3: primaryCtaText (text — e.g. "Buy")
 * 4: primaryCtaUrl (aem-content — link)
 * 5: secondaryCtaText (text — e.g. "Learn more")
 * 6: secondaryCtaUrl (aem-content — link)
 *
 * JS flow: extractConfig() → buildProductHero() → appendEvents()
 *
 * @param {Element} block
 * @returns {Object}
 */
function extractConfig(block) {
  if (!block) return {};

  const rows = [...block.children];

  return {
    image: getImageFromRow(rows[0]),
    heading: getHtmlFromRow(rows[1]),
    subheading: getHtmlFromRow(rows[2]),
    primaryCtaText: getTextFromRow(rows[3]),
    primaryCtaUrl: getLinkFromRow(rows[4]),
    secondaryCtaText: getTextFromRow(rows[5]),
    secondaryCtaUrl: getLinkFromRow(rows[6]),
  };
}

/**
 * @param {Object} config
 * @param {HTMLElement} [config.primaryCta]
 * @param {HTMLElement} [config.secondaryCta]
 */
function appendEvents(config) {
  if (config.primaryCta) {
    config.primaryCta.addEventListener('click', () => {
      /* eslint-disable no-console -- intentional debug hook */
      console.log('product-hero: primary CTA clicked');
      /* eslint-enable no-console */
    });
  }
  if (config.secondaryCta) {
    config.secondaryCta.addEventListener('click', () => {
      /* eslint-disable no-console */
      console.log('product-hero: secondary CTA clicked');
      /* eslint-enable no-console */
    });
  }
}

/**
 * Builds the product hero DOM.
 *
 * @param {Element} block
 * @param {Object} config
 */
function buildProductHero(block, config) {
  const {
    image,
    heading,
    subheading,
    primaryCtaText,
    primaryCtaUrl,
    secondaryCtaText,
    secondaryCtaUrl,
  } = config;

  const inner = document.createElement('div');
  inner.classList.add('product-hero-inner');

  /* ---- Image section (left) ---- */
  const imageSection = document.createElement('div');
  imageSection.classList.add('product-hero-image');

  if (image) {
    // image is a <picture> or <img> element returned by getImageFromRow
    imageSection.appendChild(image.cloneNode(true));
  }
  inner.appendChild(imageSection);

  /* ---- Content section (right) ---- */
  const contentSection = document.createElement('div');
  contentSection.classList.add('product-hero-content');

  if (heading) {
    const headingEl = document.createElement('div');
    headingEl.classList.add('product-hero-heading');
    headingEl.innerHTML = heading;
    contentSection.appendChild(headingEl);
  }

  if (subheading) {
    const subheadingEl = document.createElement('div');
    subheadingEl.classList.add('product-hero-subheading');
    subheadingEl.innerHTML = subheading;
    contentSection.appendChild(subheadingEl);
  }

  /* ---- CTA buttons ---- */
  const ctaWrap = document.createElement('div');
  ctaWrap.classList.add('product-hero-cta');

  if (secondaryCtaText) {
    const secondaryLink = document.createElement('a');
    secondaryLink.classList.add('product-hero-cta-secondary');
    secondaryLink.href = secondaryCtaUrl || '#';
    secondaryLink.textContent = secondaryCtaText;
    ctaWrap.appendChild(secondaryLink);
    config.secondaryCta = secondaryLink;
  }

  if (primaryCtaText) {
    const primaryLink = document.createElement('a');
    primaryLink.classList.add('product-hero-cta-primary');
    primaryLink.href = primaryCtaUrl || '#';
    primaryLink.textContent = primaryCtaText;
    ctaWrap.appendChild(primaryLink);
    config.primaryCta = primaryLink;
  }

  contentSection.appendChild(ctaWrap);
  inner.appendChild(contentSection);

  config.mainEl = inner;

  block.textContent = '';
  block.appendChild(inner);
}

/**
 * Block entry point.
 *
 * JS flow: extractConfig() → buildProductHero() → appendEvents()
 *
 * @param {Element} block
 */
export default function decorate(block) {
  const config = extractConfig(block);
  buildProductHero(block, config);
  appendEvents(config);
}
