<script setup lang="ts">
import { computed } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

export interface RetrievalSource {
  index: number;
  knowledgeBaseId?: number | null;
  documentId?: number | null;
  chunkId?: number | null;
  score?: number | null;
  rerankScore?: number | null;
  quoteText?: string | null;
}

const props = withDefaults(defineProps<{
  content?: string;
  sources?: RetrievalSource[];
}>(), {
  content: '',
  sources: () => []
});

const emit = defineEmits<{
  openSource: [source: RetrievalSource];
}>();

function escapeRawHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

const sourceMap = computed(() => {
  const next = new Map<number, RetrievalSource>();
  for (const source of props.sources || []) {
    const index = Number(source?.index);
    if (Number.isInteger(index)) {
      next.set(index, source);
    }
  }
  return next;
});

const renderedHtml = computed(() => {
  const markdown = escapeRawHtml(String(props.content || '')).replace(/\[--cite:(\d+)]/g, (_match, rawIndex) => {
    const index = Number(rawIndex);
    if (!sourceMap.value.has(index)) {
      return '';
    }

    return `<button type="button" class="citation-ref" data-cite-index="${index}" title="查看引用来源 ${index}">[${index}]</button>`;
  });

  return DOMPurify.sanitize(marked.parse(markdown) as string, {
    ADD_ATTR: ['data-cite-index']
  });
});

function handleClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  const button = target?.closest<HTMLButtonElement>('.citation-ref');
  if (!button) {
    return;
  }

  const index = Number(button.dataset.citeIndex);
  const source = sourceMap.value.get(index);
  if (source) {
    emit('openSource', source);
  }
}
</script>

<template>
  <div class="markdown-body" v-html="renderedHtml" @click="handleClick"></div>
</template>
