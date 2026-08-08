<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { listKnowledgeBases } from '../api/knowledge';

const knowledgeBases = ref([]);
const knowledgeState = ref('idle');
const knowledgeMessage = ref('');
const searchKeyword = ref('');
const sortMode = ref('recent');
const recentKnowledgeUsage = ref(readRecentKnowledgeUsage());

const visibleKnowledgeBases = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  const filtered = knowledgeBases.value.filter((base) => {
    if (!keyword) return true;
    return [base.name, base.description, base.kbCode]
      .some((value) => String(value || '').toLowerCase().includes(keyword));
  });

  return [...filtered].sort((left, right) => {
    if (sortMode.value === 'name') {
      return String(left.name || '').localeCompare(String(right.name || ''), 'zh-CN');
    }
    if (sortMode.value === 'updated') {
      return getKnowledgeUpdatedTime(right) - getKnowledgeUpdatedTime(left);
    }

    const recentDiff = getRecentUsedAt(right.id) - getRecentUsedAt(left.id);
    if (recentDiff !== 0) return recentDiff;
    return getKnowledgeUpdatedTime(right) - getKnowledgeUpdatedTime(left);
  });
});

const knowledgeCountLabel = computed(() => {
  if (knowledgeState.value === 'loading') return '...';
  return `${visibleKnowledgeBases.value.length}/${knowledgeBases.value.length}`;
});

function readRecentKnowledgeUsage() {
  try {
    return JSON.parse(window.localStorage.getItem('aoo-ai-recent-knowledge-bases') || '{}');
  } catch (error) {
    return {};
  }
}

function rememberKnowledgeBase(knowledgeBaseId) {
  recentKnowledgeUsage.value = {
    ...recentKnowledgeUsage.value,
    [knowledgeBaseId]: Date.now()
  };
  window.localStorage.setItem('aoo-ai-recent-knowledge-bases', JSON.stringify(recentKnowledgeUsage.value));
}

function getRecentUsedAt(knowledgeBaseId) {
  return Number(recentKnowledgeUsage.value[knowledgeBaseId] || 0);
}

function getKnowledgeUpdatedTime(base) {
  const value = base.latestDocumentUpdatedAt || base.updatedAt;
  const time = new Date(value || 0).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function formatDateTime(value) {
  if (!value) return '暂无更新';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function getStatusText(status) {
  const statusMap = {
    READY: '可检索',
    INDEXING: '索引中',
    EMPTY: '暂无文档',
    FAILED: '索引异常',
    UNAVAILABLE: '不可用'
  };
  return statusMap[status] || '未知状态';
}

function getStatusClass(status) {
  return {
    READY: 'ready',
    INDEXING: 'indexing',
    EMPTY: 'empty',
    FAILED: 'failed',
    UNAVAILABLE: 'failed'
  }[status] || 'empty';
}

async function loadAccessibleKnowledgeBases() {
  knowledgeState.value = 'loading';
  knowledgeMessage.value = '';

  try {
    const response = await listKnowledgeBases();
    if (!response.success) throw new Error(response.message || '知识库列表获取失败');
    knowledgeBases.value = response.data || [];
    knowledgeState.value = 'success';
  } catch (error) {
    knowledgeState.value = 'error';
    knowledgeMessage.value = error.message || '知识库列表获取失败';
  }
}

onMounted(loadAccessibleKnowledgeBases);
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>用户工作台</h2>
        <p class="muted">这里聚合普通用户可使用的知识问答入口；管理员治理能力已独立放入管理员端。</p>
      </div>
      <RouterLink class="button secondary" to="/chat">开始问答</RouterLink>
    </div>

    <article class="card accessible-knowledge-panel">
      <div class="knowledge-panel-head">
        <div>
          <p class="eyebrow">User Knowledge</p>
          <h3>可访问的知识库</h3>
          <p v-if="knowledgeMessage" class="error">{{ knowledgeMessage }}</p>
          <p v-else class="muted">点击知识库即可进入智能问答，并将问答范围限定到所选知识库。</p>
        </div>
        <p class="metric">{{ knowledgeCountLabel }}</p>
      </div>

      <div class="knowledge-list-controls">
        <label class="knowledge-search-field">
          <span class="sr-only">搜索知识库</span>
          <input v-model="searchKeyword" type="search" placeholder="搜索知识库名称、描述或编码" />
        </label>
        <label class="knowledge-sort-field">
          <span>排序</span>
          <select v-model="sortMode">
            <option value="recent">最近使用优先</option>
            <option value="updated">最近更新优先</option>
            <option value="name">名称排序</option>
          </select>
        </label>
      </div>

      <div class="knowledge-entry-list">
        <div v-if="visibleKnowledgeBases.length" class="knowledge-entry-header" aria-hidden="true">
          <span>知识库</span>
          <span>文档</span>
          <span>更新时间</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <RouterLink
          v-for="base in visibleKnowledgeBases"
          :key="base.id"
          class="knowledge-entry-row"
          :to="{ path: '/chat', query: { knowledgeBaseId: base.id } }"
          @click="rememberKnowledgeBase(base.id)"
        >
          <span class="knowledge-entry-main">
            <strong>{{ base.name }}</strong>
            <small>{{ base.description || base.kbCode || '进入该知识库范围进行问答' }}</small>
            <em v-if="getRecentUsedAt(base.id)">最近使用</em>
          </span>
          <span>{{ base.documentCount || 0 }} 个文档</span>
          <span>{{ formatDateTime(base.latestDocumentUpdatedAt || base.updatedAt) }}</span>
          <span :class="['knowledge-status-badge', getStatusClass(base.availabilityStatus)]">
            {{ getStatusText(base.availabilityStatus) }}
          </span>
          <span class="knowledge-entry-action">开始问答</span>
        </RouterLink>
        <div v-if="knowledgeState === 'loading'" class="empty-state compact">
          <p class="muted">正在加载可访问知识库...</p>
        </div>
        <div v-else-if="!visibleKnowledgeBases.length && !knowledgeMessage" class="empty-state compact">
          <p class="muted">{{ knowledgeBases.length ? '没有匹配的知识库。' : '暂无可访问知识库。' }}</p>
        </div>
      </div>
    </article>
  </section>
</template>
