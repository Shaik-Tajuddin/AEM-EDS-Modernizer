/**
 * Block Helper Utilities
 * Common utility functions for AEM block development
 */

// =============================================================================
// ROW VALUE EXTRACTORS
// =============================================================================

/**
 * Gets the first cell element from a row at given index
 * @param {Array} rows - Array of row elements
 * @param {number} index - Row index
 * @returns {Element|null} The cell element or null
 */
export function getValue(rows, index) {
  if (index >= rows.length) return null;
  const cell = rows[index]?.children[0];
  return cell || null;
}

/**
 * Gets text content from a row at given index
 * @param {Array} rows - Array of row elements
 * @param {number} index - Row index
 * @returns {string} The text content or empty string
 */
export function getText(rows, index) {
  return getValue(rows, index)?.textContent?.trim() || '';
}

/**
 * Gets HTML content from a row at given index
 * @param {Array} rows - Array of row elements
 * @param {number} index - Row index
 * @returns {string} The HTML content or empty string
 */
export function getHTML(rows, index) {
  return getValue(rows, index)?.innerHTML || '';
}

/**
 * Gets image src from a row at given index
 * @param {Array} rows - Array of row elements
 * @param {number} index - Row index
 * @returns {string|null} The image src or null
 */
export function getImage(rows, index) {
  const cell = getValue(rows, index);
  return cell?.querySelector('img')?.src
    || cell?.querySelector('picture source')?.srcset
    || null;
}

/**
 * Gets link href from a row at given index
 * @param {Array} rows - Array of row elements
 * @param {number} index - Row index
 * @returns {string|null} The link href or text content or null
 */
export function getLink(rows, index) {
  const cell = getValue(rows, index);
  return cell?.querySelector('a')?.href
    || getText(rows, index)
    || null;
}

// =============================================================================
// SINGLE ROW VALUE EXTRACTORS
// These work with individual row elements (not arrays with index)
// =============================================================================

/**
 * Gets text content from a single row element
 * @param {Element} row - The row element
 * @returns {string} The text content or empty string
 */
export function getTextFromRow(row) {
  return row?.textContent?.trim() || '';
}

/**
 * Gets HTML content from a single row element
 * Extracts from inner div if present, otherwise from row directly
 * @param {Element} row - The row element
 * @returns {string} The HTML content or empty string
 */
export function getHtmlFromRow(row) {
  const inner = row?.querySelector('div');
  if (inner) {
    return inner.innerHTML?.trim() || '';
  }
  return row?.innerHTML?.trim() || '';
}

/**
 * Gets link href from a single row element (for aem-content fields)
 * Falls back to text content if no link found
 * @param {Element} row - The row element
 * @returns {string} The href value or text content
 */
export function getLinkFromRow(row) {
  const link = row?.querySelector('a');
  return link?.getAttribute('href') || getTextFromRow(row);
}

/**
 * Gets image element or picture from a single row element
 * @param {Element} row - The row element
 * @returns {Element|null} The img or picture element
 */
export function getImageFromRow(row) {
  return row?.querySelector('picture') || row?.querySelector('img') || null;
}

/**
 * Gets boolean value from a single row element
 * @param {Element} row - The row element
 * @returns {boolean} True if text content is 'true' (case-insensitive)
 */
export function getBooleanFromRow(row) {
  return getTextFromRow(row).toLowerCase() === 'true';
}

// =============================================================================
// RESPONSIVE HELPERS
// =============================================================================

/**
 * Creates a responsive media query helper that updates on resize (width-only)
 * @param {string} query - Media query string (e.g., '(min-width: 1024px)')
 * @param {Function} callback - Function to call on change
 * @param {number} debounceMs - Debounce delay in milliseconds (default: 100)
 * @returns {Object} Object with matches property and cleanup method
 */
export function createResponsiveHelper(query, callback, debounceMs = 100) {
  const mediaQuery = window.matchMedia(query);
  let lastWidth = window.innerWidth;
  let debounceTimer = null;

  const handleResize = () => {
    // Only trigger if width changed (ignore height-only changes like mobile URL bar)
    const currentWidth = window.innerWidth;
    if (currentWidth === lastWidth) return;

    lastWidth = currentWidth;

    // Debounce the callback
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      if (callback) callback(mediaQuery.matches);
    }, debounceMs);
  };

  // Handle media query changes
  const handleMediaChange = (e) => {
    if (callback) callback(e.matches);
  };

  // Add listeners
  mediaQuery.addEventListener('change', handleMediaChange);
  window.addEventListener('resize', handleResize);

  // Return object with current state and cleanup
  return {
    get matches() {
      return mediaQuery.matches;
    },
    cleanup() {
      mediaQuery.removeEventListener('change', handleMediaChange);
      window.removeEventListener('resize', handleResize);
      if (debounceTimer) clearTimeout(debounceTimer);
    },
  };
}

/**
 * Desktop breakpoint media query helper
 * Updates on window resize (width-only) with debounce
 */
export const DESKTOP_BREAKPOINT = '(min-width: 1024px)';

/**
 * Creates a desktop detection helper
 * @param {Function} onChange - Callback when desktop state changes
 * @returns {Object} Object with matches property and cleanup method
 */
export function createDesktopHelper(onChange) {
  return createResponsiveHelper(DESKTOP_BREAKPOINT, onChange);
}

// =============================================================================
// BLOCK GROUPING HELPERS
// =============================================================================

/** Store for debounce timers keyed by grouping type */
const groupingTimers = new Map();

/**
 * Checks if an element is a block wrapper for a given block name.
 * Handles both standard wrapper class and Universal Editor DOM variations.
 *
 * @param {Element} el - Element to check
 * @param {string} blockName - Block name (e.g., 'accordion-item', 'carousel-slide')
 * @returns {boolean} True if element is a wrapper for the specified block
 */
export function isBlockWrapper(el, blockName) {
  if (!el) return false;
  // Check for wrapper class (added by EDS decorateBlock)
  if (el.classList.contains(`${blockName}-wrapper`)) return true;
  // Check if element contains the block as direct child (handles author mode)
  if (el.querySelector(`:scope > .${blockName}.block`)) return true;
  // Check if element's first child is the block itself
  const firstChild = el.firstElementChild;
  if (firstChild?.classList.contains(blockName)
    && firstChild.classList.contains('block')) {
    return true;
  }
  return false;
}

/**
 * Creates an advanced block grouping system with settings support.
 * Groups consecutive blocks of the same type into containers, and supports:
 * - Re-applying settings to existing groups (for author mode re-renders)
 * - Applying wrapper-level settings from the last item with settings enabled
 * - Robust wrapper detection for Universal Editor DOM variations
 *
 * @param {Object} options - Grouping configuration
 * @param {string} options.blockName - Block name (e.g., 'accordion-item', 'carousel-slide')
 * @param {string} options.containerClass - Class name for the container (e.g., 'accordion')
 * @param {string} options.containerRole - ARIA role for the container (default: 'region')
 * @param {string} options.containerLabel - ARIA label for the container
 * @param {number} options.debounceMs - Debounce delay in milliseconds (default: 100)
 * @param {Function} options.getWrappersFromContainer - Function to get wrapper elements
 *   from an existing container (for re-applying settings). Receives container, returns array.
 * @param {Function} options.needsSettingsApplied - Function to check if container needs settings
 *   Receives container element, returns boolean
 * @param {Function} options.applySettings - Function to apply settings to a container
 *   Receives (container, wrapperElements) arguments
 * @param {Function} options.onGroupCreated - Optional callback after a group is created
 *   Receives (container, wrapperElements) arguments, called before applySettings
 * @returns {Function} Function to call to schedule grouping
 */
export function createAdvancedBlockGrouper(options) {
  const {
    blockName,
    containerClass,
    containerRole = 'region',
    containerLabel = containerClass,
    debounceMs = 100,
    getWrappersFromContainer,
    needsSettingsApplied,
    applySettings,
    onGroupCreated,
  } = options;

  const blockSelector = `.${blockName}.block`;
  const wrapperClass = `${blockName}-wrapper`;

  /**
   * Default function to get wrappers from a container
   */
  const defaultGetWrappers = (container) => {
    const wrappers = [...container.querySelectorAll(`:scope > .${wrapperClass}`)];
    if (wrappers.length === 0) {
      container.querySelectorAll(':scope > div').forEach((div) => {
        if (div.querySelector(blockSelector) || div.classList.contains(blockName)) {
          wrappers.push(div);
        }
      });
    }
    return wrappers;
  };

  const getWrappers = getWrappersFromContainer || defaultGetWrappers;

  const groupBlocks = () => {
    // Re-apply settings to existing containers that need it
    if (needsSettingsApplied && applySettings) {
      document.querySelectorAll(`.${containerClass}`).forEach((container) => {
        if (needsSettingsApplied(container)) {
          const wrappers = getWrappers(container);
          if (wrappers.length > 0) {
            applySettings(container, wrappers);
          }
        }
      });
    }

    // Find all ungrouped blocks
    const ungroupedBlocks = document.querySelectorAll(
      `${blockSelector}:not(.${containerClass} ${blockSelector})`,
    );

    // Track processed wrappers to avoid duplicate grouping
    const processedWrappers = new Set();

    ungroupedBlocks.forEach((block) => {
      const wrapper = block.parentElement;
      if (!wrapper || processedWrappers.has(wrapper)) return;
      if (wrapper.closest(`.${containerClass}`)) return;

      // Collect consecutive block wrappers starting from this one
      const group = [];
      let current = wrapper;

      while (current && isBlockWrapper(current, blockName)) {
        if (processedWrappers.has(current) || current.closest(`.${containerClass}`)) {
          break;
        }
        group.push(current);
        processedWrappers.add(current);
        current = current.nextElementSibling;
      }

      // Create container if we have blocks
      if (group.length >= 1) {
        const container = document.createElement('div');
        container.className = containerClass;
        container.setAttribute('role', containerRole);
        container.setAttribute('aria-label', containerLabel);

        // Insert container before first wrapper
        group[0].parentElement.insertBefore(container, group[0]);

        // Move all wrappers into container and ensure wrapper class
        group.forEach((w) => {
          w.classList.add(wrapperClass);
          container.appendChild(w);
        });

        // Call onGroupCreated callback if provided
        if (onGroupCreated) {
          onGroupCreated(container, group);
        }

        // Apply settings from last item
        if (applySettings) {
          applySettings(container, group);
        }
      }
    });
  };

  // Return function that schedules the grouping with debounce
  return () => {
    const existingTimer = groupingTimers.get(containerClass);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    const timer = setTimeout(() => {
      groupBlocks();
      groupingTimers.delete(containerClass);
    }, debounceMs);

    groupingTimers.set(containerClass, timer);
  };
}

/**
 * Creates a debounced block grouping function.
 * Groups adjacent blocks of the same type into a container wrapper.
 * Uses debouncing to run once after all blocks are decorated.
 *
 * @param {Object} options - Grouping configuration
 * @param {string} options.blockWrapperSelector - Selector for block wrappers
 *   (e.g., '.accordion-item-wrapper')
 * @param {string} options.containerClass - Class name for the container (e.g., 'accordion')
 * @param {string} options.containerRole - ARIA role for the container (default: 'region')
 * @param {string} options.containerLabel - ARIA label for the container
 * @param {number} options.debounceMs - Debounce delay in milliseconds (default: 100)
 * @returns {Function} Function to call to schedule grouping
 */
export function createBlockGrouper(options) {
  const {
    blockWrapperSelector,
    containerClass,
    containerRole = 'region',
    containerLabel = containerClass,
    debounceMs = 100,
  } = options;

  const groupBlocks = () => {
    // Find all block wrappers that aren't already grouped
    const allWrappers = document.querySelectorAll(
      `${blockWrapperSelector}:not(.${containerClass} ${blockWrapperSelector})`,
    );

    allWrappers.forEach((wrapper) => {
      // Skip if already in a group
      if (wrapper.parentElement?.classList.contains(containerClass)) return;

      // Start a new group with this wrapper and all consecutive same-type siblings
      const group = [];
      let current = wrapper;

      // Collect consecutive block wrappers of the same type
      while (current && current.matches(blockWrapperSelector)) {
        group.push(current);
        current = current.nextElementSibling;
      }

      // Wrap all blocks (including single items) in a container
      if (group.length >= 1) {
        const container = document.createElement('div');
        container.className = containerClass;
        container.setAttribute('role', containerRole);
        container.setAttribute('aria-label', containerLabel);

        // Insert the container before the first wrapper
        group[0].parentElement.insertBefore(container, group[0]);

        // Move all wrappers into the container
        group.forEach((itemWrapper) => {
          container.appendChild(itemWrapper);
        });
      }
    });
  };

  // Return function that schedules the grouping with debounce
  return () => {
    // Clear any pending grouping for this type
    const existingTimer = groupingTimers.get(containerClass);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    // Schedule grouping to run after all blocks have decorated
    const timer = setTimeout(() => {
      groupBlocks();
      groupingTimers.delete(containerClass);
    }, debounceMs);

    groupingTimers.set(containerClass, timer);
  };
}

// =============================================================================
// TOGGLE/EXPAND HELPERS
// =============================================================================

/**
 * Creates a toggle function for expandable content.
 * Manages aria-expanded on trigger and aria-hidden on content.
 *
 * @param {Element} trigger - The element that triggers expand/collapse (e.g., header button)
 * @param {Element} content - The content element to show/hide
 * @returns {Function} Toggle function that can be called to expand/collapse
 */
export function createToggle(trigger, content) {
  return () => {
    const isExpanded = trigger.getAttribute('aria-expanded') === 'true';
    trigger.setAttribute('aria-expanded', String(!isExpanded));
    content.setAttribute('aria-hidden', String(isExpanded));
  };
}

/**
 * Adds toggle event listeners to an expandable component.
 * Handles click and keyboard (Enter/Space) events.
 *
 * @param {Element} trigger - The element that triggers expand/collapse
 * @param {Element} content - The content element to show/hide
 * @param {Object} options - Optional configuration
 * @param {Function} options.onToggle - Callback after toggle (receives isExpanded boolean)
 * @returns {Function} Cleanup function to remove event listeners
 */
export function addToggleListeners(trigger, content, options = {}) {
  const { onToggle } = options;

  const toggle = () => {
    const isExpanded = trigger.getAttribute('aria-expanded') === 'true';
    trigger.setAttribute('aria-expanded', String(!isExpanded));
    content.setAttribute('aria-hidden', String(isExpanded));

    if (onToggle) {
      onToggle(!isExpanded);
    }
  };

  const handleKeydown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      toggle();
    }
  };

  trigger.addEventListener('click', toggle);
  trigger.addEventListener('keydown', handleKeydown);

  // Return cleanup function
  return () => {
    trigger.removeEventListener('click', toggle);
    trigger.removeEventListener('keydown', handleKeydown);
  };
}

// =============================================================================
// ENVIRONMENT HELPERS
// =============================================================================

/**
 * Checks if the current page is in AEM Universal Editor author mode
 * @returns {boolean} True if in author mode
 */
export function isAuthorMode() {
  return window.hlx?.authorMode || false;
}
