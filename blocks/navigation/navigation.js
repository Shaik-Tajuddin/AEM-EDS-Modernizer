import {
  checkAndHandleNestedBlocks,
  replaceBlockRowsPreservingNestedBlocks,
  getTextFromBlockRow,
  getHtmlFromRow,
  coerceAuthorClasses,
  escapeHtml,
  escapeHtmlAttribute,
  franklinBlockRow,
} from '../../scripts/utilities/block-helpers.js';

function extractConfig(block) {
  if (!block) return {};
  const rows = [...block.children];
  return {
    id: getTextFromBlockRow(rows[0]),
    title: getHtmlFromRow(rows[1]),
    text: getHtmlFromRow(rows[2]),
  };
}

export default async function decorate(block) {
  await checkAndHandleNestedBlocks(block);
  const config = extractConfig(block);
  const inner = document.createElement('div');
  inner.classList.add('content-inner');
  if (config.title) {
    const h = document.createElement('div');
    h.classList.add('content-title');
    h.innerHTML = config.title;
    inner.appendChild(h);
  }
  if (config.text) {
    const p = document.createElement('div');
    p.classList.add('content-text');
    p.innerHTML = config.text;
    inner.appendChild(p);
  }
  replaceBlockRowsPreservingNestedBlocks(block, inner);
  if (config.id) block.id = config.id;
}

export function createBlock(options = {}) {
  const id = escapeHtml(options.id ?? '');
  const title = typeof options.title === 'string' ? options.title : '';
  const extra = coerceAuthorClasses(options.classes);
  const rootClasses = ['content', 'eds-block-content', extra].filter(Boolean).join(' ');
  return `<div class="${escapeHtmlAttribute(rootClasses)}">${franklinBlockRow(id)}${franklinBlockRow(title)}</div>`;
}
