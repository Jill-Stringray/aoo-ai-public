import DOMPurify from 'dompurify';
import { marked } from 'marked';

marked.use({
  gfm: true,
  breaks: true
});

/**
 * 将 Markdown 转为可安全插入页面的 HTML（用于 v-html）。
 */
export function renderMarkdown(src) {
  if (src == null) {
    return '';
  }
  const text = String(src);
  if (text.trim() === '') {
    return '';
  }
  const html = marked.parse(text);
  return DOMPurify.sanitize(html);
}
