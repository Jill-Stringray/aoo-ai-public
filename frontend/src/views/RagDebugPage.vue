<script setup>
import { onMounted, ref } from 'vue';
import { debugRagRetrieval, fetchSystemConfig } from '../api/admin';
import { getDocumentFileUrl } from '../api/knowledge';

const debugState = ref('idle');
const debugMessage = ref('');
const debugResult = ref(null);
const config = ref({
  topK: 5
});
const sampleQuestions = [
  '知识库里主要包含哪些内容？',
  '请总结一下上传文档中的关键业务规则。',
  '有哪些内容可以回答员工常见问题？',
  '知识库中有没有提到流程或制度要求？'
];
const debugForm = ref({
  question: sampleQuestions[Math.floor(Math.random() * sampleQuestions.length)],
  topK: 5,
  knowledgeBaseId: ''
});

function formatScore(value) {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(4);
}

function formatEmbeddingSample(values) {
  if (!values?.length) return '[]';
  return `[${values.map((value) => Number(value).toFixed(6)).join(', ')}]`;
}

function randomizeDebugQuestion() {
  const next = sampleQuestions[Math.floor(Math.random() * sampleQuestions.length)];
  debugForm.value.question = next;
}

function hasChunkDocument(chunk) {
  return Boolean(chunk?.documentId);
}

async function loadConfig() {
  try {
    const response = await fetchSystemConfig();
    if (!response.success) throw new Error(response.message || '系统配置获取失败');
    config.value = { ...config.value, ...(response.data || {}) };
    debugForm.value.topK = config.value.topK || 5;
  } catch (error) {
    debugMessage.value = error.message || '系统配置获取失败';
  }
}

async function runRagDebug() {
  debugState.value = 'loading';
  debugMessage.value = '';
  debugResult.value = null;
  try {
    const payload = {
      question: debugForm.value.question,
      topK: Number(debugForm.value.topK) || config.value.topK,
      knowledgeBaseId: debugForm.value.knowledgeBaseId ? Number(debugForm.value.knowledgeBaseId) : null
    };
    const response = await debugRagRetrieval(payload);
    if (!response.success) throw new Error(response.message || 'RAG检索调试失败');
    debugResult.value = response.data;
    debugState.value = 'success';
  } catch (error) {
    debugState.value = 'error';
    debugMessage.value = error.message || 'RAG检索调试失败';
  }
}

onMounted(loadConfig);
</script>

<template>
  <section class="page-section rag-debug-page">
    <div class="section-header">
      <div>
        <h2>RAG检索调试</h2>
        <p class="muted">输入一个问题，查看从 Query embedding 到最终答案的完整链路。</p>
      </div>
      <span class="status-pill">管理员端</span>
    </div>

    <article class="card rag-debug-card">
      <form class="rag-debug-form" @submit.prevent="runRagDebug">
        <label class="rag-debug-question">
          <span>输入问题</span>
          <textarea v-model="debugForm.question" rows="3" placeholder="输入一个用于调试的问题"></textarea>
        </label>
        <label>
          <span>TopK</span>
          <input v-model.number="debugForm.topK" type="number" min="1">
        </label>
        <label>
          <span>知识库ID</span>
          <input v-model="debugForm.knowledgeBaseId" type="number" min="1" placeholder="留空表示全部">
        </label>
        <div class="rag-debug-actions">
          <button type="submit" :disabled="debugState === 'loading'">
            {{ debugState === 'loading' ? '调试中...' : '开始调试' }}
          </button>
          <button type="button" class="button secondary" :disabled="debugState === 'loading'" @click="randomizeDebugQuestion">
            随机问题
          </button>
        </div>
      </form>
      <p v-if="debugMessage" class="error">{{ debugMessage }}</p>

      <div v-if="debugResult" class="rag-debug-flow">
        <section class="rag-debug-step">
          <div class="rag-step-head">
            <strong>Query embedding 结果</strong>
            <span>{{ debugResult.embeddingModel }} · {{ debugResult.embeddingDimensions }} 维</span>
          </div>
          <pre>{{ formatEmbeddingSample(debugResult.embeddingSample) }}</pre>
        </section>

        <div class="rag-flow-arrow">↓</div>

        <section class="rag-debug-step">
          <div class="rag-step-head">
            <strong>TopK chunks 返回详情</strong>
            <span>TopK {{ debugResult.topK }} · 阈值 {{ debugResult.similarityThreshold }}</span>
          </div>
          <div class="rag-chunk-list">
            <article v-for="chunk in debugResult.chunks" :key="`${chunk.rank}-${chunk.chunkId || chunk.fileName}`" class="rag-chunk-item">
              <div class="rag-chunk-meta">
                <span>#{{ chunk.rank }}</span>
                <span>score {{ formatScore(chunk.score) }}</span>
                <span>{{ chunk.fileName || '未知文档' }}</span>
                <span>page {{ chunk.pageNumber || '-' }}</span>
                <span>chunk {{ chunk.chunkId || '-' }}</span>
                <a
                  v-if="hasChunkDocument(chunk)"
                  class="rag-document-link"
                  :href="getDocumentFileUrl(chunk.documentId)"
                >
                  查看文档
                </a>
              </div>
              <p>{{ chunk.content }}</p>
            </article>
            <div v-if="!debugResult.chunks?.length" class="empty-state compact">
              <div>
                <div class="empty-icon">□</div>
                <h3>未召回切片</h3>
                <p class="muted">可以尝试降低相似度阈值或切换问题。</p>
              </div>
            </div>
          </div>
        </section>

        <div class="rag-flow-arrow">↓</div>

        <section class="rag-debug-step">
          <div class="rag-step-head">
            <strong>最终发送给LLM的context</strong>
            <span>{{ debugResult.context?.length || 0 }} 字符</span>
          </div>
          <pre>{{ debugResult.context }}</pre>
        </section>

        <div class="rag-flow-arrow">↓</div>

        <section class="rag-debug-step">
          <div class="rag-step-head">
            <strong>最终答案</strong>
            <span>{{ debugResult.question }}</span>
          </div>
          <p class="rag-answer">{{ debugResult.answer || '模型未返回答案' }}</p>
        </section>
      </div>
    </article>
  </section>
</template>
